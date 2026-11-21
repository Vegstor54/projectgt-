package com.vgst54.projectgt.multiblock;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Что может стоять на клетке структуры. Состоит из одного или нескольких правил (через {@link #or}).
 * У правила есть ключ, по которому считаются блоки во всей структуре, и пределы min/max на это число:
 * например, «не меньше 10 корпусов» или «не больше одного люка пара». Правило с пустым ключом не считается.
 */
public final class Matcher {
    /** Одно правило: условие на состояние блока и пределы для его ключа. */
    public record Rule(String key, Predicate<BlockState> test, int min, int max) {}

    private final List<Rule> rules;

    private Matcher(List<Rule> rules) {
        this.rules = rules;
    }

    /** Любой блок. */
    public static Matcher any() {
        return new Matcher(List.of(new Rule("", state -> true, 0, Integer.MAX_VALUE)));
    }

    /** Только воздух (внутренняя полость). */
    public static Matcher air() {
        return new Matcher(List.of(new Rule("", BlockState::isAir, 0, Integer.MAX_VALUE)));
    }

    /** Один из блоков, без пределов по количеству. */
    public static Matcher blocks(Block... blocks) {
        return blocks("", 0, Integer.MAX_VALUE, blocks);
    }

    /** Один из блоков с пределами по общему количеству в структуре (по ключу). */
    public static Matcher blocks(String key, int min, int max, Block... blocks) {
        return new Matcher(List.of(new Rule(key, state -> {
            for (Block block : blocks) {
                if (state.is(block)) {
                    return true;
                }
            }
            return false;
        }, min, max)));
    }

    /** Подходит либо это, либо другое (например, корпус или люк). */
    public Matcher or(Matcher other) {
        List<Rule> all = new ArrayList<>(rules);
        all.addAll(other.rules);
        return new Matcher(List.copyOf(all));
    }

    public List<Rule> rules() {
        return rules;
    }

    /** Первое подошедшее правило или null, если блок не подходит. */
    public Rule match(BlockState state) {
        for (Rule rule : rules) {
            if (rule.test().test(state)) {
                return rule;
            }
        }
        return null;
    }
}
