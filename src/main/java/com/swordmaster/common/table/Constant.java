//package com.swordmaster.common.table;
//
//public record Constant(
//        String key,
//        Type   type,
//        Object value
//) {
//    // 변수 타입 정의
//    public enum Type {
//        INTEGER,
//        DECIMAL,
//        TEXT;
//
//        public boolean matches(Object value) {
//            return switch (this) {
//                case INTEGER -> value instanceof Integer || value instanceof Long;
//                case DECIMAL -> value instanceof Number;        // 숫자형의 공통 부모 클래스
//                case TEXT    -> true;                           // 텍스트는 무조건 통과
//            };
//        }
//    }
//
//    public record Value(Type type, Object value) {}
//
//    public Value toValue() { return new Value(type, value); }
//}
