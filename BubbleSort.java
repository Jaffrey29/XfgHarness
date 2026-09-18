import java.util.Arrays;

/**
 * 冒泡排序实现（升序）。
 * 优化点：使用 swapped 标志，若某一轮未发生交换则提前结束，最好情况时间复杂度 O(n)。
 */
public class BubbleSort {

    /**
     * 对整型数组进行原地冒泡排序。
     *
     * @param arr 待排序数组，允许为 null 或长度 0
     */
    public static void sort(int[] arr) {
        if (arr == null || arr.length < 2) {
            return;
        }
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            // 每轮结束后，末尾 i 个元素已就位，无需再比较
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) {
                break; // 已有序，提前退出
            }
        }
    }

    public static void main(String[] args) {
        int[][] testCases = {
                {5, 2, 9, 1, 7},
                {1, 2, 3},          // 已有序（验证提前退出）
                {3, 2, 1},          // 逆序
                {},                 // 空数组
                {42}                // 单元素
        };
        for (int[] tc : testCases) {
            sort(tc);
            System.out.println(Arrays.toString(tc));
        }
    }
}
