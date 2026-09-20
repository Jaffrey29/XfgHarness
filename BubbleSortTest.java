import java.util.Arrays;

/**
 * 冒泡排序工具类
 */
public class BubbleSortTest {

    /**
     * 对整型数组进行冒泡排序（升序，原地排序）
     * @param arr 待排序数组，允许为 null 或空数组
     */
    public static void bubbleSort(int[] arr) {
        if (arr == null || arr.length < 2) {
            return;
        }
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false; // 本轮是否发生交换
            // 每轮结束后最大元素已"冒泡"到末尾，内层边界前移 i
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = tmp;
                    swapped = true;
                }
            }
            // 若本轮无交换，说明已有序，提前结束
            if (!swapped) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        int[] arr = {5, 2, 9, 1, 7, 3, 8, 0, 6, 4};
        System.out.println("排序前: " + Arrays.toString(arr));
        bubbleSort(arr);
        System.out.println("排序后: " + Arrays.toString(arr));

        // 边界场景验证
        int[] empty = {};
        int[] single = {42};
        int[] dup = {3, 1, 3, 1, 2};
        int[] sorted = {1, 2, 3, 4, 5};
        int[] reversed = {5, 4, 3, 2, 1};
        bubbleSort(empty);
        bubbleSort(single);
        bubbleSort(dup);
        bubbleSort(sorted);   // 应触发提前退出优化
        bubbleSort(reversed); // 最坏情况 O(n^2)
        System.out.println("空数组:     " + Arrays.toString(empty));
        System.out.println("单元素:     " + Arrays.toString(single));
        System.out.println("重复元素:   " + Arrays.toString(dup));
        System.out.println("已有序:     " + Arrays.toString(sorted));
        System.out.println("逆序:       " + Arrays.toString(reversed));
    }
}
