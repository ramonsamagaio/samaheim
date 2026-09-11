package com.samaheim.world;

import java.util.List;

/** Small deterministic runtime physics layer for the current playable client. */
public final class SandboxPhysics {
    private static final float EPS = 0.0001f;

    private SandboxPhysics() {}

    public record CircleBlocker(float x, float z, float radius) {}
    public record HorizontalResult(float x, float z, boolean blocked) {}
    public record VerticalResult(float eyeY, float velocityY, boolean grounded) {}

    public static HorizontalResult resolveHorizontal(float fromX, float fromZ, float toX, float toZ,
                                                     float playerRadius, List<CircleBlocker> blockers) {
        float x = toX;
        float z = toZ;
        boolean blocked = false;
        for (CircleBlocker blocker : blockers) {
            float minDistance = Math.max(0f, playerRadius) + Math.max(0f, blocker.radius());
            float dx = x - blocker.x();
            float dz = z - blocker.z();
            float distanceSq = dx * dx + dz * dz;
            if (distanceSq >= minDistance * minDistance) continue;
            blocked = true;
            float distance = (float) Math.sqrt(distanceSq);
            if (distance <= EPS) {
                dx = fromX - blocker.x();
                dz = fromZ - blocker.z();
                distance = (float) Math.sqrt(dx * dx + dz * dz);
                if (distance <= EPS) { dx = 1f; dz = 0f; distance = 1f; }
            }
            float scale = minDistance / distance;
            x = blocker.x() + dx * scale;
            z = blocker.z() + dz * scale;
        }
        return new HorizontalResult(x, z, blocked);
    }

    public static VerticalResult stepVertical(float currentEyeY, float velocityY, float groundY, float eyeHeight,
                                              boolean jumpRequested, float dt) {
        float frame = Math.max(0f, Math.min(dt, 0.05f));
        float floorEye = groundY + eyeHeight;
        boolean grounded = currentEyeY <= floorEye + 0.04f && velocityY <= 0f;
        float nextVelocity = velocityY;
        if (grounded && jumpRequested) {
            nextVelocity = 6.2f;
            grounded = false;
        }
        if (!grounded) nextVelocity -= 18.5f * frame;
        float nextY = currentEyeY + nextVelocity * frame;
        if (nextY <= floorEye) {
            nextY = floorEye;
            nextVelocity = 0f;
            grounded = true;
        }
        return new VerticalResult(nextY, nextVelocity, grounded);
    }
}
