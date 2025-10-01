package co;

public class ListUtil {
    
    private ListUtil() {}
    
    public static Integer sum(List<Integer> list) {
        return list.stream().mapToInt(x -> x).sum();
    }
    
    public static void ensureNotNull(List<Test> list) {
        Objects.requireNonNull(list);
    }
    
    public static void ensureArrayNotNull(Test[] arr) {
        Objects.requireNonNull(arr);
    }

}