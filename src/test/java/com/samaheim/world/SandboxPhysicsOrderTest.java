package com.samaheim.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class SandboxPhysicsOrderTest {
    @Test void reversingBlockerListKeepsSameResolvedPosition(){
        var a=new SandboxPhysics.CircleBlocker(-0.35f,0.1f,0.65f);
        var b=new SandboxPhysics.CircleBlocker(0.45f,0f,0.7f);
        var first=SandboxPhysics.resolveHorizontal(0f,-2f,0f,1f,0.42f,List.of(a,b));
        var second=SandboxPhysics.resolveHorizontal(0f,-2f,0f,1f,0.42f,List.of(b,a));
        assertEquals(first.x(),second.x(),0.002f); assertEquals(first.z(),second.z(),0.002f); assertEquals(first.blocked(),second.blocked());
    }
    @Test void diagonalSlideStillAdvancesTowardRequestedDestination(){
        var blocker=new SandboxPhysics.CircleBlocker(0f,0f,0.75f);
        var result=SandboxPhysics.resolveHorizontal(-1.2f,-1.2f,1.5f,1.5f,0.42f,List.of(blocker));
        assertTrue(result.blocked());
        float startDistance=(float)Math.sqrt(2.7f*2.7f+2.7f*2.7f);
        float dx=1.5f-result.x(),dz=1.5f-result.z();
        float endDistance=(float)Math.sqrt(dx*dx+dz*dz);
        assertTrue(endDistance<startDistance);
    }
}
