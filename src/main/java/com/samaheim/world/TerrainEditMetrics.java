package com.samaheim.world;

/** Local metrics for how far the editable surface has moved from generated terrain. */
public final class TerrainEditMetrics {
    private TerrainEditMetrics() {}

    public static float maxDisplacement(TerrainState terrain, float worldX, float worldZ, float radius) {
        if (terrain == null || !Float.isFinite(worldX) || !Float.isFinite(worldZ) || !Float.isFinite(radius) || radius <= 0f) return 0f;
        float cell = terrain.cellSize();
        int cells = terrain.cells();
        int minX = clamp((int)Math.floor((worldX-radius+terrain.halfExtent())/cell),0,cells);
        int maxX = clamp((int)Math.ceil((worldX+radius+terrain.halfExtent())/cell),0,cells);
        int minZ = clamp((int)Math.floor((worldZ-radius+terrain.halfExtent())/cell),0,cells);
        int maxZ = clamp((int)Math.ceil((worldZ+radius+terrain.halfExtent())/cell),0,cells);
        float max=0f, r2=radius*radius;
        for(int z=minZ;z<=maxZ;z++){
            float wz=terrain.vertexWorldZ(z);
            for(int x=minX;x<=maxX;x++){
                float wx=terrain.vertexWorldX(x),dx=wx-worldX,dz=wz-worldZ;
                if(dx*dx+dz*dz>r2)continue;
                max=Math.max(max,Math.abs(terrain.vertexHeight(x,z)-terrain.originalVertexHeight(x,z)));
            }
        }
        return max;
    }

    public static float averageDisplacement(TerrainState terrain, float worldX, float worldZ, float radius) {
        if (terrain == null || radius <= 0f || !Float.isFinite(radius)) return 0f;
        float cell=terrain.cellSize(); int cells=terrain.cells();
        int minX=clamp((int)Math.floor((worldX-radius+terrain.halfExtent())/cell),0,cells),maxX=clamp((int)Math.ceil((worldX+radius+terrain.halfExtent())/cell),0,cells);
        int minZ=clamp((int)Math.floor((worldZ-radius+terrain.halfExtent())/cell),0,cells),maxZ=clamp((int)Math.ceil((worldZ+radius+terrain.halfExtent())/cell),0,cells);
        float total=0f,r2=radius*radius;int count=0;
        for(int z=minZ;z<=maxZ;z++){float wz=terrain.vertexWorldZ(z);for(int x=minX;x<=maxX;x++){float wx=terrain.vertexWorldX(x),dx=wx-worldX,dz=wz-worldZ;if(dx*dx+dz*dz>r2)continue;total+=Math.abs(terrain.vertexHeight(x,z)-terrain.originalVertexHeight(x,z));count++;}}
        return count==0?0f:total/count;
    }

    private static int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
}
