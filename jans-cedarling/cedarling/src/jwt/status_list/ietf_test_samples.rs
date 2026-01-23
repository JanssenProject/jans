// This software is available under the Apache-2.0 license.
// See https://www.apache.org/licenses/LICENSE-2.0.txt for full text.
//
// Copyright (c) 2024, Gluu, Inc.

//! The tests contained here are the examples in the [Token Status List IETF draft v10](https://www.ietf.org/archive/id/draft-ietf-oauth-status-list-10.html#name-test-vectors-for-status-lis).
//!
//! In the event that there is a new RFC, the tests in the module should be checked and
//! updated if necessary.

use super::*;

const CASES_8BIT: [(usize, u8); 304] = [
    (233_478, 0),
    (52451, 1),
    (576_778, 2),
    (513_575, 3),
    (468_106, 4),
    (292_632, 5),
    (214_947, 6),
    (182_323, 7),
    (884_834, 8),
    (66653, 9),
    (62489, 10),
    (196_493, 11),
    (458_517, 12),
    (487_925, 13),
    (55649, 14),
    (416_992, 15),
    (879_796, 16),
    (462_297, 17),
    (942_059, 18),
    (583_408, 19),
    (13628, 20),
    (334_829, 21),
    (886_286, 22),
    (713_557, 23),
    (582_738, 24),
    (326_064, 25),
    (451_545, 26),
    (705_889, 27),
    (214_350, 28),
    (194_502, 29),
    (796_765, 30),
    (202_828, 31),
    (752_834, 32),
    (721_327, 33),
    (554_740, 34),
    (91122, 35),
    (963_483, 36),
    (261_779, 37),
    (793_844, 38),
    (165_255, 39),
    (614_839, 40),
    (758_403, 41),
    (403_258, 42),
    (145_867, 43),
    (96100, 44),
    (477_937, 45),
    (606_890, 46),
    (167_335, 47),
    (488_197, 48),
    (211_815, 49),
    (797_182, 50),
    (582_952, 51),
    (950_870, 52),
    (765_108, 53),
    (341_110, 54),
    (776_325, 55),
    (745_056, 56),
    (439_368, 57),
    (559_893, 58),
    (149_741, 59),
    (358_903, 60),
    (513_405, 61),
    (342_679, 62),
    (969_429, 63),
    (795_775, 64),
    (566_121, 65),
    (460_566, 66),
    (680_070, 67),
    (117_310, 68),
    (480_348, 69),
    (67319, 70),
    (661_552, 71),
    (841_303, 72),
    (561_493, 73),
    (138_807, 74),
    (442_463, 75),
    (659_927, 76),
    (445_910, 77),
    (1_046_963, 78),
    (829_700, 79),
    (962_282, 80),
    (299_623, 81),
    (555_493, 82),
    (292_826, 83),
    (517_215, 84),
    (551_009, 85),
    (898_490, 86),
    (837_603, 87),
    (759_161, 88),
    (459_948, 89),
    (290_102, 90),
    (1_034_977, 91),
    (190_650, 92),
    (98810, 93),
    (229_950, 94),
    (320_531, 95),
    (335_506, 96),
    (885_333, 97),
    (133_227, 98),
    (806_915, 99),
    (800_313, 100),
    (981_571, 101),
    (341_110, 54),
    (776_325, 55),
    (745_056, 56),
    (439_368, 57),
    (559_893, 58),
    (149_741, 59),
    (358_903, 60),
    (513_405, 61),
    (342_679, 62),
    (969_429, 63),
    (795_775, 64),
    (566_121, 65),
    (460_566, 66),
    (680_070, 67),
    (117_310, 68),
    (480_348, 69),
    (67319, 70),
    (661_552, 71),
    (841_303, 72),
    (561_493, 73),
    (138_807, 74),
    (442_463, 75),
    (659_927, 76),
    (445_910, 77),
    (1_046_963, 78),
    (829_700, 79),
    (962_282, 80),
    (299_623, 81),
    (555_493, 82),
    (292_826, 83),
    (517_215, 84),
    (551_009, 85),
    (898_490, 86),
    (837_603, 87),
    (759_161, 88),
    (459_948, 89),
    (290_102, 90),
    (1_034_977, 91),
    (190_650, 92),
    (98810, 93),
    (229_950, 94),
    (320_531, 95),
    (335_506, 96),
    (885_333, 97),
    (133_227, 98),
    (806_915, 99),
    (800_313, 100),
    (981_571, 101),
    (527_253, 102),
    (24077, 103),
    (240_232, 104),
    (559_572, 105),
    (713_399, 106),
    (233_941, 107),
    (615_514, 108),
    (911_768, 109),
    (331_680, 110),
    (951_527, 111),
    (6805, 112),
    (552_366, 113),
    (374_660, 114),
    (223_159, 115),
    (625_884, 116),
    (417_146, 117),
    (320_527, 118),
    (784_154, 119),
    (338_792, 120),
    (1199, 121),
    (679_804, 122),
    (1_024_680, 123),
    (40845, 124),
    (234_603, 125),
    (761_225, 126),
    (644_903, 127),
    (502_167, 128),
    (121_477, 129),
    (505_144, 130),
    (165_165, 131),
    (179_628, 132),
    (1_019_195, 133),
    (145_149, 134),
    (263_738, 135),
    (269_256, 136),
    (996_739, 137),
    (346_296, 138),
    (555_864, 139),
    (887_384, 140),
    (444_173, 141),
    (421_844, 142),
    (653_716, 143),
    (836_747, 144),
    (783_119, 145),
    (918_762, 146),
    (946_835, 147),
    (253_764, 148),
    (519_895, 149),
    (471_224, 150),
    (134_272, 151),
    (709_016, 152),
    (44112, 153),
    (482_585, 154),
    (461_829, 155),
    (15080, 156),
    (148_883, 157),
    (123_467, 158),
    (480_125, 159),
    (141_348, 160),
    (65877, 161),
    (692_958, 162),
    (148_598, 163),
    (499_131, 164),
    (584_009, 165),
    (1_017_987, 166),
    (449_287, 167),
    (277_478, 168),
    (991_262, 169),
    (509_602, 170),
    (991_896, 171),
    (853_666, 172),
    (399_318, 173),
    (197_815, 174),
    (203_278, 175),
    (903_979, 176),
    (743_015, 177),
    (888_308, 178),
    (862_143, 179),
    (979_421, 180),
    (113_605, 181),
    (206_397, 182),
    (127_113, 183),
    (844_358, 184),
    (711_569, 185),
    (229_153, 186),
    (521_470, 187),
    (401_793, 188),
    (398_896, 189),
    (940_810, 190),
    (293_983, 191),
    (884_749, 192),
    (384_802, 193),
    (584_151, 194),
    (970_201, 195),
    (523_882, 196),
    (158_093, 197),
    (929_312, 198),
    (205_329, 199),
    (106_091, 200),
    (30949, 201),
    (195_586, 202),
    (495_723, 203),
    (348_779, 204),
    (852_312, 205),
    (1_018_463, 206),
    (1_009_481, 207),
    (448_260, 208),
    (841_042, 209),
    (122_967, 210),
    (345_269, 211),
    (794_764, 212),
    (4520, 213),
    (818_773, 214),
    (556_171, 215),
    (954_221, 216),
    (598_210, 217),
    (887_110, 218),
    (1_020_623, 219),
    (324_632, 220),
    (398_244, 221),
    (622_241, 222),
    (456_551, 223),
    (122_648, 224),
    (127_837, 225),
    (657_676, 226),
    (119_884, 227),
    (105_156, 228),
    (999_897, 229),
    (330_160, 230),
    (119_285, 231),
    (168_005, 232),
    (389_703, 233),
    (143_699, 234),
    (142_524, 235),
    (493_258, 236),
    (846_778, 237),
    (251_420, 238),
    (516_351, 239),
    (83344, 240),
    (171_931, 241),
    (879_178, 242),
    (663_475, 243),
    (546_865, 244),
    (428_362, 245),
    (658_891, 246),
    (500_560, 247),
    (557_034, 248),
    (830_023, 249),
    (274_471, 250),
    (629_139, 251),
    (958_869, 252),
    (663_071, 253),
    (152_133, 254),
    (19535, 255),
];

#[test]
fn test_get_status_1_bit() {
    let status_list_src = "eNrt3AENwCAMAEGogklACtKQPg9LugC9k_ACvreiogEAAKkeCQAAAAAAAAAAAAAAAAAAAIBylgQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAXG9IAAAAAAAAAPwsJAAAAAAAAAAAAAAAvhsSAAAAAAAAAAAA7KpLAAAAAAAAAAAAAAAAAAAAAJsLCQAAAAAAAAAAADjelAAAAAAAAAAAKjDMAQAAAACAZC8L2AEb";

    let status_list = StatusList::parse(status_list_src, 1).unwrap();

    let cases = [
        (0, 1),
        (1993, 1),
        (25460, 1),
        (159_495, 1),
        (495_669, 1),
        (554_353, 1),
        (645_645, 1),
        (723_232, 1),
        (854_545, 1),
        (934_534, 1),
        (1_000_345, 1),
    ];

    for (idx, expected_status) in cases {
        let got_status = status_list
            .get_status(idx)
            .unwrap_or_else(|_| panic!("failed to get status for idx: {idx}"));
        assert_eq!(
            got_status,
            expected_status.into(),
            "assertion failed for idx: {idx}"
        );
    }
}

#[test]
fn test_get_status_2_bit() {
    let status_list_src = "eNrt2zENACEQAEEuoaBABP5VIO01fCjIHTMStt9ovGVIAAAAAABAbiEBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEB5WwIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAID0ugQAAAAAAAAAAAAAAAAAQG12SgAAAAAAAAAAAAAAAAAAAAAAAAAAAOCSIQEAAAAAAAAAAAAAAAAAAAAAAAD8ExIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAwJEuAQAAAAAAAAAAAAAAAAAAAAAAAMB9SwIAAAAAAAAAAAAAAAAAAACoYUoAAAAAAAAAAAAAAEBqH81gAQw";

    let status_list = StatusList::parse(status_list_src, 2).unwrap();

    let cases = [
        (0, 1),
        (1993, 2),
        (25460, 1),
        (159_495, 3),
        (495_669, 1),
        (554_353, 1),
        (645_645, 2),
        (723_232, 1),
        (854_545, 1),
        (934_534, 2),
        (1_000_345, 3),
    ];

    for (idx, expected_status) in cases {
        let got_status = status_list
            .get_status(idx)
            .unwrap_or_else(|_| panic!("failed to get status for idx: {idx}"));
        assert_eq!(
            got_status,
            expected_status.into(),
            "assertion failed for idx: {idx}"
        );
    }
}

#[test]
fn test_get_status_4_bit() {
    let status_list_src = "eNrt0EENgDAQADAIHwImkIIEJEwCUpCEBBQRHOy35Li1EjoOQGabAgAAAAAAAAAAAAAAAAAAACC1SQEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABADrsCAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADoxaEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIIoCgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACArpwKAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGhqVkAzlwIAAAAAiGVRAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABx3AoAgLpVAQAAAAAAAAAAAAAAwM89rwMAAAAAAAAAAAjsA9xMBMA";

    let status_list = StatusList::parse(status_list_src, 4).unwrap();

    let cases = [
        (0, 1),
        (1993, 2),
        (35460, 3),
        (459_495, 4),
        (595_669, 5),
        (754_353, 6),
        (845_645, 7),
        (923_232, 8),
        (924_445, 9),
        (934_534, 10),
        (1_004_534, 11),
        (1_000_345, 12),
        (1_030_203, 13),
        (1_030_204, 14),
        (1_030_205, 15),
    ];

    for (idx, expected_status) in cases {
        let got_status = status_list
            .get_status(idx)
            .unwrap_or_else(|_| panic!("failed to get status for idx: {idx}"));
        assert_eq!(
            got_status,
            expected_status.into(),
            "assertion failed for idx: {idx}"
        );
    }
}

#[test]
fn test_get_status_8_bit() {
    let status_list_src = "eNrt0WOQM2kYhtGsbdu2bdu2bdu2bdu2bdu2jVnU1my-SWYm6U5enFPVf7ue97orFYAo7CQBAACQuuckAABStqUEAAAAAAAAtN6wEgAE71QJAAAAAIrwhwQAAAAAAdtAAgAAAAAAACLwkAQAAAAAAAAAAACUaFcJAACAeJwkAQAAAAAAAABQvL4kAAAAWmJwCQAAAAAAAAjAwBIAAAB06ywJoDKQBARpfgkAAAAAAAAAAAAAAAAAAACo50sJAAAAAAAAAOiRcSQAAAAAgAJNKgEAAG23mgQAAAAAAECw3pUAQvegBAAAAAAAAADduE4CAAAAyjSvBAAQiw8koHjvSABAb-wlARCONyVoxtMSZOd0CQAAAOjWDRKQmLckAAAAAACysLYEQGcnSAAAAAAQooUlAABI15kSAIH5RAIgLB9LABC4_SUgGZNIAABAmM6RoLbTJIASzCIBAEAhfpcAAAAAAABquk8CAAAAAAAAaJl9SvvzBOICAFWmkIBgfSgBAAAANOgrCQAAAAAAAADStK8EAAC03gASAAAAAAAAAADFWFUCAAAAMjOaBEADHpYAQjCIBADduFwCAAAAAGitMSSI3BUSAECOHpAA6IHrJQAAAAAAsjeVBAAAKRpVAorWvwQAAAAAAAAAkKRtJAAAAAAAgCbcLAF0bXUJAAAAoF02kYDg7CYBAAAAAEB6NpQAAAAAAAAAAAAAAEr1uQQAAF06VgIAAAAAAAAAqDaeBAAQqgMkAAAAAABogQMlAAAAAAAa87MEAAAQiwslAAAAAAAAAAAAAAAAMrOyBAAAiekv-hcsY0Sgne6QAAAAAAAgaUtJAAAAAAAAAAAAAAAAAAAAAAAAAADwt-07vjVkAAAAgDy8KgFAUEaSAAAAAJL3vgQAWdhcAgAAoBHDSUDo1pQAAACI2o4SAABZm14CALoyuwQAAPznGQkgZwdLAAAQukclAAAAAAAAAAAAgKbMKgEAAAAAAAAAAAAAAAAAAECftpYAAAAAAAAAAAAACnaXBAAAAADk7iMJAAAAAAAAAABqe00CAnGbBBG4TAIAgFDdKgFAXCaWAAAAAAAAAAAAAAAAAKAJQwR72XbGAQAAAKAhh0sAAAAAAABQgO8kAAAAAAAAAAAAACAaM0kAAAC5W0QCAIJ3mAQAxGwxCQAA6nhSAsjZBRIAANEbWQIAAAAAaJE3JACAwA0qAUBIVpKAlphbAiAPp0iQnKEkAAAAAAAgBP1KAAAAdOl4CQAAAAAAAPjLZBIAAG10RtrPm8_CAEBMTpYAAAAAAIjQYBL8z5QSAAAAAEDYPpUAACAsj0gAAADQkHMlAAjHDxIA0Lg9JQAAgHDsLQEAAABAQS6WAAAAgLjNFs2l_RgLAIAEfCEBlGZZCQAAaIHjJACgtlskAAAozb0SAAAAVFtfAgAAAAAAAAAAAAAAAAAAAAAAAKDDtxIAAAAAVZaTAKB5W0kAANCAsSUgJ0tL0GqHSNBbL0gAZflRAgCARG0kQXNmlgCABiwkAQAAAEB25pIAAAAAAAAAAAAAoFh9SwAAAAAAADWNmOSrpjFsEoaRgDKcF9Q1dxsEAAAAAAAAAAAAAAAAgPZ6SQIAAAAAAAAAgChMLgEAAAAAAAAAqZlQAsK2qQQAAAAAAAD06XUJAAAAqG9bCQAAgLD9IgEAAAAAAAAAAAAAAAAAAEBNe0gAAAAAAAAAAEBPHSEBAAAAlOZtCYA4fS8B0GFRCQAo0gISAOTgNwmC840EAAAAAAAAAAAAAAAAAAAAUJydJfjXPBIAAAAAAAAAAAAAAABk6WwJAAAAAAAAAAAAAAAAqG8UCQAAgPpOlAAAIA83SQAANWwc9HUjGAgAAAAAAACAusaSAAAAAAAAAAAAAAAAAAAAAAAAAAAAqHKVBACQjxklAAAAAAAAAKBHxpQAAAAAACBME0lAdlaUAACyt7sEAAAA0Nl0EgAAAAAAAAAAAABA-8wgAQAAAAAAAKU4SgKgUtlBAgAAAAAAAAAAgMCMLwEE51kJICdzSgCJGl2CsE0tAQAA0L11JQAAAAAAAAjUOhIAAAAAAAAAAAAAAGTqeQkAAAAAAAAAAAAAKM8SEjTrJwkAAAAAAACocqQEULgVJAAAACjDUxJUKgtKAAAAqbpRAgCA0n0mAQAAAABAGzwmAUCTLpUAAAAAAAAAAEjZNRIAAAAAAAAAAAAAAAAAAAAA8I-vJaAlhpQAAAAAAHrvzjJ-OqCuuVlLAojP8BJAr70sQZVDJYAgXS0BAAAAAAAAAAAAtMnyEgAAAAAAFONKCQAAAAAAAADorc0kAAAAAAAAgDqOlgAAAAAAAAAAAADIwv0SAAAAAAAAAAAAAADBuV0CIFVDSwAAAABAAI6RAAAAAGIwrQSEZAsJAABouRclAAAAAKDDrxIAAAA0bkkJgFiMKwEAAAAAAHQyhwRk7h4JAAAAAAAAAAAgatdKAACUYj0JAAAAAAAAAAAAQnORBLTFJRIAAAAAkIaDJAAAAJryngQAAAAAAAAAAAA98oQEAAAAAAAAAEC2zpcgWY9LQKL2kwAgGK9IAAAAAPHaRQIAAAAAAAAAAADIxyoSAAAAAAAAAAAAAADQFotLAECz_gQ1PX-B";

    let status_list = StatusList::parse(status_list_src, 8).unwrap();

    for (idx, expected_status) in CASES_8BIT {
        let got_status = status_list
            .get_status(idx)
            .unwrap_or_else(|_| panic!("failed to get status for idx: {idx}"));
        assert_eq!(
            got_status,
            expected_status.into(),
            "assertion failed for idx: {idx}"
        );
    }
}
