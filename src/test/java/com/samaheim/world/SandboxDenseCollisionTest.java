package com.samaheim.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class SandboxDenseCollisionTest {
    @Test void denseClusterNeverLeavesPlayerInsideAnyBlocker(){
        List<SandboxPhysics.CircleBlocker> blockers=List.of(
                new SandboxPhysics.CircleBlocker(-0.25f,0f,0.7f),
                new SandboxPhysics.CircleBlocker(0.25f,0f,0.7f),
                new SandboxPhysics.CircleBlocker(0f,0.35f,0.7f),
                new SandboxPhysics.CircleBlocker(0f,-0.35f,0.7f));
        var r=SandboxPhysics.resolveHorizontal(0f,-2f,0f,0f,0.42f,blockers);
        assertTrue(r.blocked());
        assertTrue(Float.isFinite(r.x())&&Float.isFinite(r.z()));
        for(var b:blockers){
            float dx=r.x()-b.x(),dz=r.z()-b.z();
            assertTrue(Math.sqrt(dx*dx+dz*dz)>=1.10f,"player must finish outside dense blocker cluster");
        }
    }
}
