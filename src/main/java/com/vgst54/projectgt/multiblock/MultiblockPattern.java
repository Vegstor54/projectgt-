package com.vgst54.projectgt.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Форма мультиблока, как FactoryBlockPattern в GT. Задаётся слоями (aisle) и таблицей символов (where).
 *
 * Слой это один срез структуры в глубину: список строк сверху вниз, в строке символы слева направо,
 * как смотрит игрок на лицо контроллера. Слои идут от ближнего к дальнему (от игрока вглубь).
 * Контроллер отмечается символом '@' (ровно один на всю структуру), пробел значит «любой блок».
 * Структура строится от контроллера и поворачивается вместе с ним (только по горизонтали).
 */
public final class MultiblockPattern {
    public static final char CONTROLLER = '@';

    /** Итог проверки: собрана ли структура, сообщение и количество блоков по ключам. */
    public record Result(boolean formed, Component message, Map<String, Integer> counts) {}

    private final List<String[]> aisles;
    private final Map<Character, Matcher> where;
    private final int controllerAisle;
    private final int controllerRow;
    private final int controllerCol;
    private final Map<String, int[]> limits = new HashMap<>();

    private MultiblockPattern(List<String[]> aisles, Map<Character, Matcher> where) {
        this.aisles = aisles;
        this.where = where;
        int ca = -1;
        int cr = -1;
        int cc = -1;
        for (int a = 0; a < aisles.size(); a++) {
            String[] rows = aisles.get(a);
            for (int r = 0; r < rows.length; r++) {
                int col = rows[r].indexOf(CONTROLLER);
                if (col >= 0) {
                    ca = a;
                    cr = r;
                    cc = col;
                }
            }
        }
        if (ca < 0) {
            throw new IllegalStateException("В паттерне нет контроллера '@'");
        }
        this.controllerAisle = ca;
        this.controllerRow = cr;
        this.controllerCol = cc;
        for (char symbol : symbols()) {
            if (symbol == CONTROLLER || symbol == ' ') {
                continue;
            }
            Matcher matcher = where.get(symbol);
            if (matcher == null) {
                throw new IllegalStateException("Для символа '" + symbol + "' нет where()");
            }
            for (Matcher.Rule rule : matcher.rules()) {
                if (!rule.key().isEmpty()) {
                    limits.put(rule.key(), new int[] {rule.min(), rule.max()});
                }
            }
        }
    }

    private List<Character> symbols() {
        List<Character> list = new ArrayList<>();
        for (String[] rows : aisles) {
            for (String row : rows) {
                for (char c : row.toCharArray()) {
                    list.add(c);
                }
            }
        }
        return list;
    }

    public static Builder start() {
        return new Builder();
    }

    /** Мировая позиция клетки паттерна (слой, строка, столбец) для контроллера в controllerPos с лицом facing. */
    private BlockPos positionOf(BlockPos controllerPos, Direction facing, int aisle, int row, int col) {
        Direction back = facing.getOpposite();
        // Игрок смотрит на лицо контроллера против его направления, его правая рука это против часовой от лица.
        Direction right = facing.getCounterClockWise();
        return controllerPos
                .relative(back, aisle - controllerAisle)
                .relative(right, col - controllerCol)
                .above(controllerRow - row);
    }

    /** Проверяет структуру вокруг контроллера. */
    public Result check(Level level, BlockPos controllerPos, Direction facing) {
        Map<String, Integer> counts = new HashMap<>();
        for (int a = 0; a < aisles.size(); a++) {
            String[] rows = aisles.get(a);
            for (int r = 0; r < rows.length; r++) {
                String row = rows[r];
                for (int c = 0; c < row.length(); c++) {
                    char symbol = row.charAt(c);
                    if (symbol == CONTROLLER || symbol == ' ') {
                        continue;
                    }
                    BlockPos pos = positionOf(controllerPos, facing, a, r, c);
                    BlockState state = level.getBlockState(pos);
                    Matcher.Rule rule = where.get(symbol).match(state);
                    if (rule == null) {
                        return new Result(false, Component.translatable("message.projectgt.multiblock.wrong_block",
                                state.getBlock().getName(), pos.getX(), pos.getY(), pos.getZ()), counts);
                    }
                    if (!rule.key().isEmpty()) {
                        counts.merge(rule.key(), 1, Integer::sum);
                    }
                }
            }
        }
        for (Map.Entry<String, int[]> entry : limits.entrySet()) {
            int count = counts.getOrDefault(entry.getKey(), 0);
            int[] limit = entry.getValue();
            if (count < limit[0] || count > limit[1]) {
                return new Result(false, Component.translatable("message.projectgt.multiblock.wrong_count",
                        entry.getKey(), count, limit[0], limit[1]), counts);
            }
        }
        return new Result(true, Component.translatable("message.projectgt.multiblock.formed"), counts);
    }

    public static final class Builder {
        private final List<String[]> aisles = new ArrayList<>();
        private final Map<Character, Matcher> where = new HashMap<>();

        /** Добавляет слой: строки сверху вниз. */
        public Builder aisle(String... rows) {
            aisles.add(rows);
            return this;
        }

        public Builder where(char symbol, Matcher matcher) {
            where.put(symbol, matcher);
            return this;
        }

        public MultiblockPattern build() {
            return new MultiblockPattern(List.copyOf(aisles), Map.copyOf(where));
        }
    }
}
