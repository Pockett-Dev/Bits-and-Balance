package org.onenonly.bitsandbalance.fabric.mobs;

import net.minecraft.world.entity.monster.Phantom;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class MountedPhantomAttackContext {
	private static final ThreadLocal<Deque<UUID>> PHANTOM_UUID_STACK = ThreadLocal.withInitial(ArrayDeque::new);

	private MountedPhantomAttackContext() {
	}

	public static void push(UUID phantomUuid) {
		if (phantomUuid == null) return;
		PHANTOM_UUID_STACK.get().push(phantomUuid);
	}

	public static void popIfMatches(UUID phantomUuid) {
		if (phantomUuid == null) return;
		Deque<UUID> stack = PHANTOM_UUID_STACK.get();
		if (!stack.isEmpty() && phantomUuid.equals(stack.peek())) {
			stack.pop();
			if (stack.isEmpty()) {
				PHANTOM_UUID_STACK.remove();
			}
		}
	}

	public static boolean matches(Phantom phantom) {
		if (phantom == null) return false;
		Deque<UUID> stack = PHANTOM_UUID_STACK.get();
		return !stack.isEmpty() && phantom.getUUID().equals(stack.peek());
	}
}
