package com.samaheim.world;

import java.util.List;

/** Small deterministic runtime physics layer for the current playable client. */
public final class SandboxPhysics {
    private static final float EPS=0.0001f,COLLISION_SKIN=0.012f,MAX_HORIZONTAL_STEP=0.28f;
    private static final int MAX_DEPENETRATION_PASSES=8;
    private static final float GRAVITY=18.5f,JUMP_SPEED=6.2f,TERMINAL_FALL_SPEED=-32f,GROUND_SNAP_DOWN=0.32f;
    private SandboxPhysics(){}
    public record CircleBlocker(float x,float z,float radius){}
    public record HorizontalResult(float x,float z,boolean blocked){}
    public record VerticalResult(float eyeY,float velocityY,boolean grounded){}

    public static HorizontalResult resolveHorizontal(float fromX,float fromZ,float toX,float toZ,float playerRadius,List<CircleBlocker> blockers){
        if(!Float.isFinite(fromX)||!Float.isFinite(fromZ)||!Float.isFinite(toX)||!Float.isFinite(toZ))return new HorizontalResult(fromX,fromZ,true);
        List<CircleBlocker> safe=blockers==null?List.of():blockers; float radius=Float.isFinite(playerRadius)?Math.max(0f,playerRadius):0f;
        float dx=toX-fromX,dz=toZ-fromZ,d=(float)Math.sqrt(dx*dx+dz*dz); int steps=Math.max(1,(int)Math.ceil(d/MAX_HORIZONTAL_STEP));
        float sx=dx/steps,sz=dz/steps,x=fromX,z=fromZ; boolean blocked=false;
        for(int i=0;i<steps;i++){ Depenetration dep=depenetrate(x+sx,z+sz,x,z,radius,safe); if(dep.blocked){blocked=true;
                Depenetration xo=depenetrate(x+sx,z,x,z,radius,safe),zo=depenetrate(x,z+sz,x,z,radius,safe);
                float fp=progressSq(x,z,dep.x,dep.z),xp=progressSq(x,z,xo.x,xo.z),zp=progressSq(x,z,zo.x,zo.z);
                if(xp>fp&&xp>=zp)dep=xo; else if(zp>fp)dep=zo;} x=dep.x;z=dep.z; }
        return new HorizontalResult(x,z,blocked);
    }

    private static Depenetration depenetrate(float cx,float cz,float fx,float fz,float playerRadius,List<CircleBlocker> blockers){
        float x=cx,z=cz;boolean blocked=false;
        for(int pass=0;pass<MAX_DEPENETRATION_PASSES;pass++){ CircleBlocker chosen=null;float chosenPen=0f;
            for(CircleBlocker b:blockers){ if(!valid(b))continue;float min=playerRadius+Math.max(0f,b.radius())+COLLISION_SKIN;if(min<=EPS)continue;
                float dx=x-b.x(),dz=z-b.z(),dist=(float)Math.sqrt(Math.max(0f,dx*dx+dz*dz)),pen=min-dist;
                if(pen>EPS&&(chosen==null||pen>chosenPen+EPS||(Math.abs(pen-chosenPen)<=EPS&&tieBefore(b,chosen)))){chosen=b;chosenPen=pen;} }
            if(chosen==null)break;blocked=true;float dx=x-chosen.x(),dz=z-chosen.z(),dist=(float)Math.sqrt(dx*dx+dz*dz);
            if(dist<=EPS){dx=fx-chosen.x();dz=fz-chosen.z();dist=(float)Math.sqrt(dx*dx+dz*dz);if(dist<=EPS){dx=1f;dz=0f;dist=1f;}}
            float min=playerRadius+Math.max(0f,chosen.radius())+COLLISION_SKIN,push=min/dist;x=chosen.x()+dx*push;z=chosen.z()+dz*push; }
        return new Depenetration(x,z,blocked);
    }
    private static boolean valid(CircleBlocker b){return b!=null&&Float.isFinite(b.x())&&Float.isFinite(b.z())&&Float.isFinite(b.radius());}
    private static boolean tieBefore(CircleBlocker a,CircleBlocker b){return a.x()<b.x()||(a.x()==b.x()&&(a.z()<b.z()||(a.z()==b.z()&&a.radius()<b.radius())));}

    public static VerticalResult stepVertical(float currentEyeY,float velocityY,float groundY,float eyeHeight,boolean jumpRequested,float dt){
        if(!Float.isFinite(currentEyeY)||!Float.isFinite(velocityY)||!Float.isFinite(groundY)||!Float.isFinite(eyeHeight)||!Float.isFinite(dt)){
            float g=Float.isFinite(groundY)?groundY:0f,h=Float.isFinite(eyeHeight)?Math.max(0f,eyeHeight):1.72f;return new VerticalResult(g+h,0f,true);}
        float frame=Math.max(0f,Math.min(dt,0.05f)),floor=groundY+Math.max(0f,eyeHeight); float separation=currentEyeY-floor;
        boolean nearGround=separation<=0.055f || (velocityY<=0.15f&&separation>=0f&&separation<=GROUND_SNAP_DOWN);
        boolean grounded=nearGround&&velocityY<=0.15f; float v=velocityY;
        if(grounded){if(jumpRequested){currentEyeY=Math.max(currentEyeY,floor);v=JUMP_SPEED;grounded=false;}else{currentEyeY=floor;v=0f;}}
        if(!grounded)v=Math.max(TERMINAL_FALL_SPEED,v-GRAVITY*frame);float y=currentEyeY+v*frame;if(y<=floor){y=floor;v=0f;grounded=true;}
        return new VerticalResult(y,v,grounded);
    }
    public static float terminalFallSpeed(){return TERMINAL_FALL_SPEED;} public static float groundSnapDown(){return GROUND_SNAP_DOWN;}
    private static float progressSq(float x,float z,float nx,float nz){float dx=nx-x,dz=nz-z;return dx*dx+dz*dz;}
    private record Depenetration(float x,float z,boolean blocked){}
}
