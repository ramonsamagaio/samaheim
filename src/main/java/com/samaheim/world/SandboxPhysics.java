package com.samaheim.world;

import java.util.List;

/** Small deterministic runtime physics layer for the current playable client. */
public final class SandboxPhysics {
    private static final float EPS = 0.0001f;
    private static final float MAX_HORIZONTAL_STEP = 0.35f;
    private static final int MAX_DEPENETRATION_PASSES = 4;

    private SandboxPhysics() {}

    public record CircleBlocker(float x, float z, float radius) {}
    public record HorizontalResult(float x, float z, boolean blocked) {}
    public record VerticalResult(float eyeY, float velocityY, boolean grounded) {}

    /** Swept horizontal collision prevents fast runtime movement tunneling through blockers. */
    public static HorizontalResult resolveHorizontal(float fromX, float fromZ, float toX, float toZ,
                                                     float playerRadius, List<CircleBlocker> blockers) {
        if (!Float.isFinite(fromX) || !Float.isFinite(fromZ) || !Float.isFinite(toX) || !Float.isFinite(toZ)) {
            return new HorizontalResult(fromX, fromZ, true);
        }
        List<CircleBlocker> safeBlockers = blockers == null ? List.of() : blockers;
        float dxTotal = toX - fromX;
        float dzTotal = toZ - fromZ;
        float distance = (float) Math.sqrt(dxTotal * dxTotal + dzTotal * dzTotal);
        int steps = Math.max(1, (int) Math.ceil(distance / MAX_HORIZONTAL_STEP));
        float stepX = dxTotal / steps;
        float stepZ = dzTotal / steps;
        float x = fromX;
        float z = fromZ;
        boolean blocked = false;

        for (int step = 0; step < steps; step++) {
            float candidateX = x + stepX;
            float candidateZ = z + stepZ;
            Depenetration dep = depenetrate(candidateX, candidateZ, x, z, Math.max(0f, playerRadius), safeBlockers);
            x = dep.x;
            z = dep.z;
            blocked |= dep.blocked;
        }
        return new HorizontalResult(x, z, blocked);
    }

    private static Depenetration depenetrate(float candidateX, float candidateZ, float fallbackX, float fallbackZ,
                                             float playerRadius, List<CircleBlocker> blockers) {
        float x = candidateX;
        float z = candidateZ;
        boolean blocked = false;
        for (int pass = 0; pass < MAX_DEPENETRATION_PASSES; pass++) {
            boolean changed = false;
            for (CircleBlocker blocker : blockers) {
                if (blocker == null || !Float.isFinite(blocker.x()) || !Float.isFinite(blocker.z()) || !Float.isFinite(blocker.radius())) continue;
                float minDistance = playerRadius + Math.max(0f, blocker.radius());
                if (minDistance <= EPS) continue;
                float dx = x - blocker.x();
                float dz = z - blocker.z();
                float distanceSq = dx * dx + dz * dz;
                if (distanceSq >= minDistance * minDistance - EPS) continue;
                blocked = true;
                changed = true;
                float distance = (float) Math.sqrt(Math.max(0f, distanceSq));
                if (distance <= EPS) {
                    dx = fallbackX - blocker.x();
                    dz = fallbackZ - blocker.z();
                    distance = (float) Math.sqrt(dx * dx + dz * dz);
                    if (distance <= EPS) { dx = 1f; dz = 0f; distance = 1f; }
                }
                float push = (minDistance + 0.001f) / distance;
                x = blocker.x() + dx * push;
                z = blocker.z() + dz * push;
            }
            if (!changed) break;
        }
        return new Depenetration(x, z, blocked);
    }

    /** Stable vertical integration against the edited heightfield. */
    public static VerticalResult stepVertical(float currentEyeY, float velocityY, float groundY, float eyeHeight,
                                              boolean jumpRequested, float dt) {
        if (!Float.isFinite(currentEyeY) || !Float.isFinite(velocityY) || !Float.isFinite(groundY)
                || !Float.isFinite(eyeHeight) || !Float.isFinite(dt)) {
            float safeGround = Float.isFinite(groundY) ? groundY : 0f;
            float safeHeight = Float.isFinite(eyeHeight) ? Math.max(0f, eyeHeight) : 1.72f;
            return new VerticalResult(safeGround + safeHeight, 0f, true);
        }
        float frame = Math.max(0f, Math.min(dt, 0.05f));
        float floorEye = groundY + Math.max(0f, eyeHeight);
        boolean grounded = currentEyeY <= floorEye + 0.055f && velocityY <= 0.15f;
        float nextVelocity = velocityY;
        if (grounded) {
            if (jumpRequested) {
                currentEyeY = Math.max(currentEyeY, floorEye);
                nextVelocity = 6.2f;
                grounded = false;
            } else {
                currentEyeY = floorEye;
                nextVelocity = 0f;
            }
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

    private record Depenetration(float x, float z, boolean blocked) {}
}
