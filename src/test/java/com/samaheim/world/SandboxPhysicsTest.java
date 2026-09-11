package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SandboxPhysicsTest {
    @Test void movementStopsOutsideCircularObstacle() { var r=SandboxPhysics.resolveHorizontal(0f,0f,2f,0f,0.45f,List.of(new SandboxPhysics.CircleBlocker(2f,0f,0.55f))); assertTrue(r.blocked()); float dx=r.x()-2f,dz=r.z(); assertTrue(Math.sqrt(dx*dx+dz*dz)>=0.999); }
    @Test void clearMovementPassesThroughUnchanged() { var r=SandboxPhysics.resolveHorizontal(0f,0f,1f,1f,0.4f,List.of(new SandboxPhysics.CircleBlocker(8f,8f,1f))); assertFalse(r.blocked()); assertEquals(1f,r.x(),0.0001f); assertEquals(1f,r.z(),0.0001f); }
    @Test void fastMovementCannotTunnelThroughTreeSizedBlocker() { var r=SandboxPhysics.resolveHorizontal(-4f,0f,4f,0f,0.42f,List.of(new SandboxPhysics.CircleBlocker(0f,0f,0.55f))); assertTrue(r.blocked()); assertTrue(r.x()<0f); assertTrue(Math.abs(r.x())>=0.96f); }
    @Test void diagonalMovementSlidesAroundObstacleInsteadOfFreezing() { var r=SandboxPhysics.resolveHorizontal(-1.4f,-1f,1.2f,1.8f,0.42f,List.of(new SandboxPhysics.CircleBlocker(0f,0f,0.6f))); assertTrue(r.blocked()); assertTrue(r.z()>-0.4f); }
    @Test void overlappingBlockersAreResolvedWithoutNaN() { var r=SandboxPhysics.resolveHorizontal(0f,-2f,0f,0f,0.42f,List.of(new SandboxPhysics.CircleBlocker(-0.35f,0f,0.6f),new SandboxPhysics.CircleBlocker(0.35f,0f,0.6f))); assertTrue(r.blocked()); assertTrue(Float.isFinite(r.x())); assertTrue(Float.isFinite(r.z())); }
    @Test void collisionSkinLeavesTinySeparationFromObstacle() { var r=SandboxPhysics.resolveHorizontal(-2f,0f,0f,0f,0.42f,List.of(new SandboxPhysics.CircleBlocker(0f,0f,0.58f))); float d=(float)Math.sqrt(r.x()*r.x()+r.z()*r.z()); assertTrue(d>=1.005f); }
    @Test void invalidRadiusFailsSoftInsteadOfProducingNaN() { var r=SandboxPhysics.resolveHorizontal(0f,0f,1f,0f,Float.NaN,List.of(new SandboxPhysics.CircleBlocker(5f,0f,1f))); assertTrue(Float.isFinite(r.x())); assertTrue(Float.isFinite(r.z())); }
    @Test void jumpLeavesGroundAndGravityBringsPlayerBack() { float h=1.72f,y=h,v=0f; var first=SandboxPhysics.stepVertical(y,v,0f,h,true,1f/60f); assertFalse(first.grounded()); assertTrue(first.eyeY()>h); y=first.eyeY(); v=first.velocityY(); SandboxPhysics.VerticalResult s=first; for(int i=0;i<240;i++){ s=SandboxPhysics.stepVertical(y,v,0f,h,false,1f/60f); y=s.eyeY(); v=s.velocityY(); } assertTrue(s.grounded()); assertEquals(h,s.eyeY(),0.001f); }
    @Test void fallingTracksRaisedTerrain() { var s=SandboxPhysics.stepVertical(5f,-2f,2f,1.72f,false,0.05f); assertTrue(s.eyeY()>=3.72f); }
    @Test void hugeFrameHitchCannotTunnelBelowGround() { var s=SandboxPhysics.stepVertical(20f,-50f,4f,1.72f,false,0.8f); assertTrue(s.eyeY()>=5.72f); }
    @Test void corruptedVerticalInputFailsSafeToGround() { var s=SandboxPhysics.stepVertical(Float.NaN,Float.NaN,3f,1.72f,true,Float.NaN); assertTrue(s.grounded()); assertEquals(4.72f,s.eyeY(),0.001f); }
    @Test void longFallCannotAcceleratePastTerminalVelocity() { float y=200f,v=0f; SandboxPhysics.VerticalResult s=null; for(int i=0;i<240;i++){ s=SandboxPhysics.stepVertical(y,v,-1000f,1.72f,false,0.05f); y=s.eyeY(); v=s.velocityY(); } assertNotNull(s); assertTrue(s.velocityY()>=SandboxPhysics.terminalFallSpeed()-0.001f); }
}
