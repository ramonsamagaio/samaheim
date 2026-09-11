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
        assertEquals(first.x(),second.x(),0.002f);
        assertEquals(first.z(),second.z(),0.002f);
        assertEquals(first.blocked(),second.blocked());
    }
}
