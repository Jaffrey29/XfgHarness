public class BubbleSort {

    /**
     * 冒泡排序（升序，原地排序，稳定）
     * 优化：如果某一轮没有发生交换，说明已有序，提前结束。
     */
    public static void sort(int[] arr) {
        if (arr == null || arr.length < 2) {
            return;
        }
        int n = arr.length;
        for (int i = 0; i < n - 1; i++) {
            boolean swapped = false;
            // 每轮结束后，最大的元素已"冒泡"到末尾，内层无需再比较末尾 i 个
            for (int j = 0; j < n - 1 - i; j++) {
                if (arr[j] > arr[j + 1]) {
                    int tmp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = tmp;
                    swapped = true;
                }
            }
            if (!swapped) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        int[] arr = {5, 2, 9, 1, 7, 3, 8, 2};
        sort(arr);
        System.out.println(java.util.Arrays.toString(arr));
        // 期望输出: [1, 2, 2, 3, 5, 7, 8, 9]
    }
}
