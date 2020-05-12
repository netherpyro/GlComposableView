package com.netherpyro.glcv

/**
 * @author mmikhailov on 2019-12-06.
 */
enum class AspectRatio(val value: Float, val title: String) {

    RATIO_18_9(18f / 9f, "18:9"),
    RATIO_16_9(16f / 9f, "16:9"),
    RATIO_3_2(3f / 2f, "3:2"),
    RATIO_5_4(5f / 4f, "5:4"),
    RATIO_1_1(1f, "1:1"),
    RATIO_4_5(4f / 5f, "4:5"),
    RATIO_2_3(2f / 3f, "2:3"),
    RATIO_9_16(9f / 16f, "9:16"),
    RATIO_9_18(9f / 18f, "9:18")
}