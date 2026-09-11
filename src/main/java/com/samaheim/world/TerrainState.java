package com.samaheim.world;

import java.util.Arrays;
import java.util.Locale;

/** Editable generated terrain with coarse sculpting plus a fine leveling layer. */
public final class TerrainState {
    private static final float EPSILON = 0.0005f;
    private static final float MAX_LEVEL_OFFSET = 1.0f;
    private static final float TARGET_SAMPLE_SPACING = 0.75f;

    private final float halfExtent;
    private final int cells;
    private final int width;
    private final float cellSize;
    private final float maxDelta;
    private final float[] baseHeights;
    private final float[] sculptDeltas;
    private final float[] levelOffsets;
    private final float[] smoothScratch;

    private int dirtyMinX = Integer.MAX_VALUE;
    private int dirtyMinZ = Integer.MAX_VALUE;
    private int dirtyMaxX = Integer.MIN_VALUE;
    private int dirtyMaxZ = Integer.MIN_VALUE;

    public TerrainState(long seed, float halfExtent, int minimumCells, float maxDelta) {
        if (!Float.isFinite(halfExtent) || !Float.isFinite(maxDelta) || halfExtent <= 0f || minimumCells < 2 || maxDelta <= 0f) throw new IllegalArgumentException("Invalid terrain dimensions");
        this.halfExtent = halfExtent;
        int targetCells = (int) Math.ceil((halfExtent * 2f) / TARGET_SAMPLE_SPACING);
        this.cells = Math.max(minimumCells, targetCells);
        this.width = cells + 1;
        this.cellSize = (halfExtent * 2f) / cells;
        this.maxDelta = maxDelta;
        this.baseHeights = new float[width * width];
        this.sculptDeltas = new float[width * width];
        this.levelOffsets = new float[width * width];
        this.smoothScratch = new float[width * width];
        for (int z = 0; z < width; z++) {
            float worldZ = -halfExtent + z * cellSize;
            for (int x = 0; x < width; x++) baseHeights[index(x, z)] = WorldMath.height(seed, -halfExtent + x * cellSize, worldZ);
        }
    }

    public int cells() { return cells; }
    public int width() { return width; }
    public float cellSize() { return cellSize; }
    public float halfExtent() { return halfExtent; }
    public float maxDelta() { return maxDelta; }
    public float maxLevelOffset() { return MAX_LEVEL_OFFSET; }
    public float vertexWorldX(int x) { return -halfExtent + x * cellSize; }
    public float vertexWorldZ(int z) { return -halfExtent + z * cellSize; }
    public float vertexHeight(int x, int z) { int i=index(clampIndex(x),clampIndex(z)); return baseHeights[i]+sculptDeltas[i]+levelOffsets[i]; }
    public float originalVertexHeight(int x, int z) { return baseHeights[index(clampIndex(x),clampIndex(z))]; }
    public float sculptedVertexHeight(int x, int z) { int i=index(clampIndex(x),clampIndex(z)); return baseHeights[i]+sculptDeltas[i]; }

    public float sampleHeight(float worldX, float worldZ) {
        if (!Float.isFinite(worldX) || !Float.isFinite(worldZ)) return 0f;
        float gx=(clamp(worldX,-halfExtent,halfExtent)+halfExtent)/cellSize, gz=(clamp(worldZ,-halfExtent,halfExtent)+halfExtent)/cellSize;
        int x0=Math.min(cells-1,Math.max(0,(int)Math.floor(gx))), z0=Math.min(cells-1,Math.max(0,(int)Math.floor(gz)));
        int x1=Math.min(cells,x0+1), z1=Math.min(cells,z0+1); float tx=clamp(gx-x0,0f,1f),tz=clamp(gz-z0,0f,1f);
        float a=lerp(vertexHeight(x0,z0),vertexHeight(x1,z0),tx), b=lerp(vertexHeight(x0,z1),vertexHeight(x1,z1),tx); return lerp(a,b,tz);
    }

    public int raise(float worldX,float worldZ,float radius,float amount){return sculptBrush(worldX,worldZ,radius,Math.abs(amount));}
    public int lower(float worldX,float worldZ,float radius,float amount){return sculptBrush(worldX,worldZ,radius,-Math.abs(amount));}

    public int level(float worldX,float worldZ,float radius,float targetHeight,float maxStep){
        if(!validBrush(worldX,worldZ,radius,maxStep)||!Float.isFinite(targetHeight))return 0; int changed=0;
        int minX=gridMin(worldX-radius),maxX=gridMax(worldX+radius),minZ=gridMin(worldZ-radius),maxZ=gridMax(worldZ+radius);
        for(int z=minZ;z<=maxZ;z++){float wz=vertexWorldZ(z);for(int x=minX;x<=maxX;x++){float wx=vertexWorldX(x),distance=distance(wx,wz,worldX,worldZ);if(distance>radius)continue;
            float weight=brushWeight(distance,radius);int i=index(x,z);float sculpted=baseHeights[i]+sculptDeltas[i];float wantedFinal=clamp(targetHeight,sculpted-MAX_LEVEL_OFFSET,sculpted+MAX_LEVEL_OFFSET);
            float wantedOffset=wantedFinal-sculpted,difference=wantedOffset-levelOffsets[i],step=clamp(difference,-maxStep*weight,maxStep*weight);
            if(Math.abs(step)>EPSILON&&setLevelOffset(i,levelOffsets[i]+step)){markDirty(x,z);changed++;}}}return changed;
    }

    /** Smooths only the fine hoe layer and copies only the brush neighborhood into scratch. */
    public int smooth(float worldX,float worldZ,float radius,float strength){
        if(!validBrush(worldX,worldZ,radius,strength))return 0;
        int minX=gridMin(worldX-radius),maxX=gridMax(worldX+radius),minZ=gridMin(worldZ-radius),maxZ=gridMax(worldZ+radius);
        int copyMinX=Math.max(0,minX-1),copyMaxX=Math.min(cells,maxX+1),copyMinZ=Math.max(0,minZ-1),copyMaxZ=Math.min(cells,maxZ+1);
        for(int z=copyMinZ;z<=copyMaxZ;z++)for(int x=copyMinX;x<=copyMaxX;x++){int i=index(x,z);smoothScratch[i]=baseHeights[i]+sculptDeltas[i]+levelOffsets[i];}
        int changed=0;
        for(int z=minZ;z<=maxZ;z++){float wz=vertexWorldZ(z);for(int x=minX;x<=maxX;x++){float wx=vertexWorldX(x),dist=distance(wx,wz,worldX,worldZ);if(dist>radius)continue;
            float avg=neighborhoodAverage(x,z,smoothScratch);int i=index(x,z);float sculpted=baseHeights[i]+sculptDeltas[i];float targetOffset=clamp(avg-sculpted,-MAX_LEVEL_OFFSET,MAX_LEVEL_OFFSET);
            float amount=strength*brushWeight(dist,radius),next=moveToward(levelOffsets[i],targetOffset,amount);if(setLevelOffset(i,next)){markDirty(x,z);changed++;}}}
        return changed;
    }

    public int restore(float worldX,float worldZ,float radius,float strength){
        if(!validBrush(worldX,worldZ,radius,strength))return 0;int changed=0;int minX=gridMin(worldX-radius),maxX=gridMax(worldX+radius),minZ=gridMin(worldZ-radius),maxZ=gridMax(worldZ+radius);
        for(int z=minZ;z<=maxZ;z++){float wz=vertexWorldZ(z);for(int x=minX;x<=maxX;x++){float wx=vertexWorldX(x),dist=distance(wx,wz,worldX,worldZ);if(dist>radius)continue;int i=index(x,z);float amount=strength*brushWeight(dist,radius);
            float ns=moveToward(sculptDeltas[i],0f,amount),nl=moveToward(levelOffsets[i],0f,amount);boolean sc=Math.abs(ns-sculptDeltas[i])>EPSILON&&setSculptDelta(i,ns),lc=Math.abs(nl-levelOffsets[i])>EPSILON&&setLevelOffset(i,nl);
            if(sc||lc){markDirty(x,z);changed++;}}}return changed;
    }

    public float slopeDegrees(float worldX,float worldZ,float sampleRadius){float r=Math.max(cellSize,Math.abs(sampleRadius));float dx=sampleHeight(worldX+r,worldZ)-sampleHeight(worldX-r,worldZ),dz=sampleHeight(worldX,worldZ+r)-sampleHeight(worldX,worldZ-r);return(float)Math.toDegrees(Math.atan2((float)Math.sqrt(dx*dx+dz*dz),2f*r));}

    public float heightVariation(float worldX,float worldZ,float radius){
        if(!Float.isFinite(worldX)||!Float.isFinite(worldZ)||!Float.isFinite(radius)||radius<=0f)return Float.POSITIVE_INFINITY;
        int minX=gridMin(worldX-radius),maxX=gridMax(worldX+radius),minZ=gridMin(worldZ-radius),maxZ=gridMax(worldZ+radius);float min=Float.POSITIVE_INFINITY,max=Float.NEGATIVE_INFINITY;
        for(int z=minZ;z<=maxZ;z++)for(int x=minX;x<=maxX;x++){float h=vertexHeight(x,z);min=Math.min(min,h);max=Math.max(max,h);}return max-min;
    }

    public boolean isBuildable(float worldX,float worldZ,float radius,float maxSlopeDegrees,float maxVariation){return Float.isFinite(maxSlopeDegrees)&&Float.isFinite(maxVariation)&&maxSlopeDegrees>=0f&&maxVariation>=0f&&slopeDegrees(worldX,worldZ,Math.max(radius*0.5f,cellSize))<=maxSlopeDegrees&&heightVariation(worldX,worldZ,radius)<=maxVariation;}
    public int modifiedSampleCount(){int count=0;for(int i=0;i<sculptDeltas.length;i++)if(Math.abs(sculptDeltas[i])>EPSILON||Math.abs(levelOffsets[i])>EPSILON)count++;return count;}

    public DirtyRegion consumeDirtyRegion(){if(dirtyMinX==Integer.MAX_VALUE)return null;DirtyRegion r=new DirtyRegion(dirtyMinX,dirtyMinZ,dirtyMaxX,dirtyMaxZ);resetDirtyRegion();return r;}
    public void clearEdits(){Arrays.fill(sculptDeltas,0f);Arrays.fill(levelOffsets,0f);markDirty(0,0);markDirty(cells,cells);}

    public String encodeDeltas(){StringBuilder out=new StringBuilder();for(int i=0;i<sculptDeltas.length;i++){if(Math.abs(sculptDeltas[i])>EPSILON)append(out,'s',i,sculptDeltas[i]);if(Math.abs(levelOffsets[i])>EPSILON)append(out,'l',i,levelOffsets[i]);}return out.toString();}

    public void decodeDeltas(String encoded){Arrays.fill(sculptDeltas,0f);Arrays.fill(levelOffsets,0f);resetDirtyRegion();if(encoded==null||encoded.isBlank())return;
        for(String entry:encoded.split(";")){String[] parts=entry.split(":");try{int i;float value;if(parts.length==3){i=Integer.parseInt(parts[1]);value=Float.parseFloat(parts[2]);if(i<0||i>=sculptDeltas.length||!Float.isFinite(value))continue;if("s".equals(parts[0]))sculptDeltas[i]=clamp(value,-maxDelta,maxDelta);else if("l".equals(parts[0]))levelOffsets[i]=clamp(value,-MAX_LEVEL_OFFSET,MAX_LEVEL_OFFSET);else continue;}else if(parts.length==2){i=Integer.parseInt(parts[0]);value=Float.parseFloat(parts[1]);if(i<0||i>=sculptDeltas.length||!Float.isFinite(value))continue;sculptDeltas[i]=clamp(value,-maxDelta,maxDelta);}else continue;markDirty(i%width,i/width);}catch(NumberFormatException ignored){}}
    }

    private int sculptBrush(float worldX,float worldZ,float radius,float amount){
        if(!validBrush(worldX,worldZ,radius,Math.abs(amount))||amount==0f)return 0;int changed=0;int minX=gridMin(worldX-radius),maxX=gridMax(worldX+radius),minZ=gridMin(worldZ-radius),maxZ=gridMax(worldZ+radius);
        for(int z=minZ;z<=maxZ;z++){float wz=vertexWorldZ(z);for(int x=minX;x<=maxX;x++){float wx=vertexWorldX(x),dist=distance(wx,wz,worldX,worldZ);if(dist>radius)continue;int i=index(x,z);float weight=brushWeight(dist,radius);
            if(setSculptDelta(i,sculptDeltas[i]+amount*weight)){levelOffsets[i]=moveToward(levelOffsets[i],0f,Math.abs(amount)*weight);markDirty(x,z);changed++;}}}return changed;
    }

    private float neighborhoodAverage(int x,int z,float[] source){float total=0f;int count=0;for(int dz=-1;dz<=1;dz++){int nz=clampIndex(z+dz);for(int dx=-1;dx<=1;dx++){int nx=clampIndex(x+dx);total+=source[index(nx,nz)];count++;}}return total/count;}
    private boolean setSculptDelta(int i,float next){float c=clamp(next,-maxDelta,maxDelta);if(Math.abs(c-sculptDeltas[i])<=EPSILON)return false;sculptDeltas[i]=c;return true;}
    private boolean setLevelOffset(int i,float next){float c=clamp(next,-MAX_LEVEL_OFFSET,MAX_LEVEL_OFFSET);if(Math.abs(c-levelOffsets[i])<=EPSILON)return false;levelOffsets[i]=c;return true;}
    private void markDirty(int x,int z){dirtyMinX=Math.min(dirtyMinX,clampIndex(x));dirtyMinZ=Math.min(dirtyMinZ,clampIndex(z));dirtyMaxX=Math.max(dirtyMaxX,clampIndex(x));dirtyMaxZ=Math.max(dirtyMaxZ,clampIndex(z));}
    private void resetDirtyRegion(){dirtyMinX=Integer.MAX_VALUE;dirtyMinZ=Integer.MAX_VALUE;dirtyMaxX=Integer.MIN_VALUE;dirtyMaxZ=Integer.MIN_VALUE;}
    private boolean validBrush(float worldX,float worldZ,float radius,float strength){return Float.isFinite(worldX)&&Float.isFinite(worldZ)&&Float.isFinite(radius)&&Float.isFinite(strength)&&radius>0f&&strength>0f;}
    private static void append(StringBuilder out,char layer,int index,float value){if(out.length()>0)out.append(';');out.append(layer).append(':').append(index).append(':').append(String.format(Locale.ROOT,"%.4f",value));}
    private int gridMin(float world){return clampIndex((int)Math.floor((world+halfExtent)/cellSize));} private int gridMax(float world){return clampIndex((int)Math.ceil((world+halfExtent)/cellSize));}
    private int clampIndex(int value){return Math.max(0,Math.min(cells,value));} private int index(int x,int z){return z*width+x;}
    private static float brushWeight(float distance,float radius){float n=clamp(distance/radius,0f,1f);if(n<=0.35f)return 1f;float edge=(n-0.35f)/0.65f,s=edge*edge*(3f-2f*edge);return 1f-s;}
    private static float distance(float ax,float az,float bx,float bz){float dx=ax-bx,dz=az-bz;return(float)Math.sqrt(dx*dx+dz*dz);} private static float moveToward(float value,float target,float amount){return value<target?Math.min(value+amount,target):Math.max(value-amount,target);}
    private static float lerp(float a,float b,float t){return a+(b-a)*t;} private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}

    public record DirtyRegion(int minX,int minZ,int maxX,int maxZ){public int width(){return maxX-minX+1;}public int height(){return maxZ-minZ+1;}public int sampleCount(){return width()*height();}}
}
