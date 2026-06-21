package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveNodeResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveTreeNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DriveTreeBuilderTest {

    @Test
    void buildsNestedTreeWithDirectoriesFirst() {
        DriveNodeResponse file = node(3L, 1L, "b.txt", false);
        DriveNodeResponse childDir = node(2L, 1L, "a-dir", true);
        DriveNodeResponse root = node(1L, 0L, "root", true);
        DriveNodeResponse rootFile = node(4L, 0L, "a.txt", false);

        List<DriveTreeNode> tree = DriveTreeBuilder.build(List.of(file, childDir, rootFile, root));

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).node().name()).isEqualTo("root");
        assertThat(tree.get(0).children()).extracting(n -> n.node().name())
                .containsExactly("a-dir", "b.txt");
        assertThat(tree.get(1).node().name()).isEqualTo("a.txt");
    }

    private static DriveNodeResponse node(Long id, Long parentId, String name, boolean directory) {
        return new DriveNodeResponse(id, 3L, 1, null, parentId, name, directory,
                directory ? null : id + 100, null, directory ? null : 10L, null, null, null);
    }
}
