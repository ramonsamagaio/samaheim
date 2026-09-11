package com.samaheim.world;

import java.util.Locale;

/** Deterministic terrain-tool rules used directly by the playable client. */
public final class TerraformToolSystem {
    public static final float DEFAULT_RADIUS=2.8f,MIN_RADIUS=1.6f,MAX_RADIUS=4.2f;
    private static final float SMOOTH_NOOP_ROUGHNESS=0.035f,LEVEL_NOOP_ROUGHNESS=0.055f,LEVEL_NOOP_HEIGHT_ERROR=0.045f,EDGE_BRUSH_MARGIN=0.55f,RESTORE_NOOP_EDIT=0.025f;
    private TerraformToolSystem(){}
    public enum Mode{
        LEVEL("Level ground",0,4f),RAISE("Raise ground",2,7f),LOWER("Dig ground",0,6f),SMOOTH("Smooth ground",0,5f),RESTORE("Restore ground",0,3f);
        private final String label;private final int stoneCost;private final float staminaCost;
        Mode(String l,int s,float st){label=l;stoneCost=s;staminaCost=st;}public String label(){return label;}public int stoneCost(){return stoneCost;}public float staminaCost(){return staminaCost;}
        public Mode next(){Mode[]v=values();return v[(ordinal()+1)%v.length];}
    }
    public static Result apply(TerrainState terrain,Mode mode,float worldX,float worldZ,float standingHeight,float requestedRadius,int stoneAvailable,float staminaAvailable){
        if(terrain==null||mode==null)return Result.denied("No terrain tool context.");
        if(!Float.isFinite(worldX)||!Float.isFinite(worldZ)||!Float.isFinite(standingHeight)||!Float.isFinite(requestedRadius)||requestedRadius<=0f)return Result.denied("Invalid terrain target.");
        float half=terrain.halfExtent();if(Math.abs(worldX)>half||Math.abs(worldZ)>half)return Result.denied("That ground is outside the world.");
        float radius=clamp(requestedRadius,MIN_RADIUS,MAX_RADIUS),border=radius*EDGE_BRUSH_MARGIN;if(half-Math.abs(worldX)<border||half-Math.abs(worldZ)<border)return Result.denied("The terrain tool cannot shape the world edge.");
        float areaScale=clamp((radius*radius)/(DEFAULT_RADIUS*DEFAULT_RADIUS),0.5f,2.25f),depthScale=clamp((float)Math.sqrt(DEFAULT_RADIUS/radius),0.82f,1.18f);
        int quotedStone=mode==Mode.RAISE?Math.max(1,Math.round(mode.stoneCost()*areaScale)):0;
        float slope=terrain.slopeDegrees(worldX,worldZ,Math.max(0.7f,radius*0.35f)),rough=terrain.heightVariation(worldX,worldZ,Math.max(0.8f,radius*0.45f)),center=terrain.sampleHeight(worldX,worldZ);
        float editAverage=mode==Mode.RESTORE?TerrainEditMetrics.averageDisplacement(terrain,worldX,worldZ,radius):0f;
        if(mode==Mode.SMOOTH&&rough<=SMOOTH_NOOP_ROUGHNESS)return Result.denied("The ground is already smooth here.");
        if(mode==Mode.LEVEL&&rough<=LEVEL_NOOP_ROUGHNESS&&Math.abs(center-standingHeight)<=LEVEL_NOOP_HEIGHT_ERROR)return Result.denied("The ground is already level with your footing.");
        if(mode==Mode.RESTORE&&editAverage<=RESTORE_NOOP_EDIT)return Result.denied("The ground already matches its original shape here.");
        float effort=1f;if(mode==Mode.RAISE||mode==Mode.LOWER)effort+=clamp(slope/70f,0f,0.28f);if(mode==Mode.LEVEL||mode==Mode.SMOOTH)effort+=clamp(rough/4f,0f,0.22f);
        if(mode==Mode.RESTORE)effort=0.72f+clamp(editAverage/terrain.maxDelta(),0f,0.55f);
        float quotedStamina=mode.staminaCost()*(0.72f+0.28f*areaScale)*effort;
        if(stoneAvailable<quotedStone)return Result.denied("Raise ground needs "+quotedStone+" stone for this brush size.");if(staminaAvailable+0.0001f<quotedStamina)return Result.denied("Too exhausted to shape the ground.");
        int changed=switch(mode){
            case LEVEL->terrain.level(worldX,worldZ,radius,standingHeight,clamp((0.52f+rough*0.08f)*depthScale,0.45f,0.82f));
            case RAISE->terrain.raise(worldX,worldZ,radius,clamp((0.58f-slope*0.0025f)*depthScale,0.38f,0.66f));
            case LOWER->terrain.lower(worldX,worldZ,radius,clamp((0.60f-slope*0.002f)*depthScale,0.40f,0.69f));
            case SMOOTH->terrain.smooth(worldX,worldZ,radius,clamp((0.28f+rough*0.05f)*depthScale,0.24f,0.52f));
            case RESTORE->terrain.restore(worldX,worldZ,radius,clamp((0.56f+editAverage*0.08f)*depthScale,0.48f,0.88f));};
        if(changed==0)return new Result(false,0,0,0f,"The ground cannot move further here.");
        int expected=Math.max(1,Math.round((float)(Math.PI*radius*radius)/(terrain.cellSize()*terrain.cellSize())));float productive=clamp(changed/(float)expected,0.2f,1f);
        int stoneCost=quotedStone==0?0:Math.max(1,Math.round(quotedStone*productive));float staminaCost=quotedStamina*(0.55f+0.45f*productive);
        String detail=switch(mode){case LEVEL->" leveled around your footing.";case RAISE->" packed into a stable mound.";case LOWER->" cut into the earth.";case SMOOTH->" softened the sharp edges.";case RESTORE->" pulled toward its original shape.";};
        String cost=stoneCost>0?" -"+stoneCost+" stone, -"+format(staminaCost)+" stamina":" -"+format(staminaCost)+" stamina";
        return new Result(true,changed,stoneCost,staminaCost,mode.label()+detail+cost);
    }
    private static String format(float v){return String.format(Locale.ROOT,"%.1f",v);}private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    public record Result(boolean applied,int changedSamples,int stoneSpent,float staminaSpent,String message){private static Result denied(String m){return new Result(false,0,0,0f,m);}}
}
