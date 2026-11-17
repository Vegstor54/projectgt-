package com.vgst54.projectgt.material;

/**
 * Параметры инструментов из материала. Материал без ToolStats инструментов не получает
 * (так отсеиваются мягкие металлы вроде олова).
 *
 * @param durability прочность
 * @param speed      скорость добычи (для кирки, топора, лопаты, мотыги)
 * @param tier       уровень добычи: 0 дерево, 1 камень, 2 железо, 3 алмаз
 * @param metal      твёрдый металл: годится для кувалды, пилы, ключа и т.д. Не металл (дерево) даёт только мягкие инструменты
 */
public record ToolStats(int durability, float speed, int tier, boolean metal) {}
