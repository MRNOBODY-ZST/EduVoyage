package cn.edu.shmtu.eduvoyage.drive.service;

import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveNodeResponse;
import cn.edu.shmtu.eduvoyage.drive.dto.DriveDtos.DriveTreeNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds a stable nested tree from flat drive nodes. The method is pure so tree
 * ordering and orphan handling can be tested without database I/O.
 */
public final class DriveTreeBuilder {

    private DriveTreeBuilder() {
    }

    public static List<DriveTreeNode> build(List<DriveNodeResponse> nodes) {
        Map<Long, MutableNode> index = new LinkedHashMap<>();
        for (DriveNodeResponse node : nodes) {
            index.put(node.id(), new MutableNode(node));
        }

        List<MutableNode> roots = new ArrayList<>();
        for (MutableNode current : index.values()) {
            Long parentId = current.node.parentId();
            MutableNode parent = parentId == null || parentId == 0 ? null : index.get(parentId);
            if (parent == null) {
                roots.add(current);
            } else {
                parent.children.add(current);
            }
        }
        roots.sort(DriveTreeBuilder::compare);
        Set<Long> visiting = new HashSet<>();
        return roots.stream().map(root -> freeze(root, visiting)).toList();
    }

    private static DriveTreeNode freeze(MutableNode node, Set<Long> visiting) {
        if (!visiting.add(node.node.id())) {
            return new DriveTreeNode(node.node, List.of());
        }
        node.children.sort(DriveTreeBuilder::compare);
        List<DriveTreeNode> children = node.children.stream()
                .map(child -> freeze(child, visiting))
                .toList();
        visiting.remove(node.node.id());
        return new DriveTreeNode(node.node, children);
    }

    private static int compare(MutableNode left, MutableNode right) {
        return Comparator.comparing((MutableNode n) -> !n.node.directory())
                .thenComparing(n -> n.node.name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(n -> n.node.id())
                .compare(left, right);
    }

    private static final class MutableNode {
        private final DriveNodeResponse node;
        private final List<MutableNode> children = new ArrayList<>();

        private MutableNode(DriveNodeResponse node) {
            this.node = node;
        }
    }
}
