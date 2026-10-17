package online.longlian.app.service.app.impl.taskinstance;

import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SlotLabelsTest {

    @Test
    void singleSlot_hasNoSerialStep() {
        List<ItemStageNodeStatusBO> nodes = List.of(node(1, "翻译", 1, 1));

        SlotLabels.Position position = SlotLabels.of(1L, nodes);

        assertThat(position.slotIndex()).isEqualTo(1);
        assertThat(position.slotTotal()).isEqualTo(1);
        assertThat(position.serialStep()).isNull();
    }

    @Test
    void parallelGroup_ordersByParallelSortThenId() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(3, "嵌字", 1, 3),
                node(1, "翻译", 1, 1),
                node(2, "校对", 1, 2));

        SlotLabels.Position second = SlotLabels.of(2L, nodes);

        assertThat(second.slotIndex()).isEqualTo(2);
        assertThat(second.slotTotal()).isEqualTo(3);
        assertThat(second.serialStep()).isNull();
    }

    @Test
    void serialSameName_countsStageOccurrence() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "校对", 1, 1),
                node(2, "翻译", 2, 1),
                node(3, "校对", 3, 1));

        SlotLabels.Position third = SlotLabels.of(3L, nodes);

        assertThat(third.slotIndex()).isEqualTo(1);
        assertThat(third.slotTotal()).isEqualTo(1);
        assertThat(third.serialStep()).isEqualTo(2);
    }

    @Test
    void serialSameNameInsideParallelGroup_keepsBoth() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "校对", 1, 1),
                node(2, "校对", 2, 1),
                node(3, "校对", 2, 2));

        SlotLabels.Position right = SlotLabels.of(3L, nodes);

        assertThat(right.slotIndex()).isEqualTo(2);
        assertThat(right.slotTotal()).isEqualTo(2);
        assertThat(right.serialStep()).isEqualTo(2);
    }

    @Test
    void sameNameOnlyInsideOneParallelGroup_isNotSerial() {
        List<ItemStageNodeStatusBO> nodes = List.of(
                node(1, "翻译", 1, 1),
                node(2, "翻译", 1, 2),
                node(3, "翻译", 1, 3));

        assertThat(SlotLabels.of(2L, nodes).serialStep()).isNull();
        assertThat(SlotLabels.of(2L, nodes).slotIndex()).isEqualTo(2);
        assertThat(SlotLabels.of(2L, nodes).slotTotal()).isEqualTo(3);
    }

    @Test
    void missingNode_fallsBackToSingleSlot() {
        assertThat(SlotLabels.of(9L, List.of(node(1, "翻译", 1, 1))))
                .isEqualTo(new SlotLabels.Position(1, 1, null));
    }

    private static ItemStageNodeStatusBO node(long id, String name, int sort, int parallelSort) {
        return new ItemStageNodeStatusBO(10L, id, name, sort, parallelSort, false);
    }
}
