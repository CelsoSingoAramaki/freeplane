/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file author is Christian Foltin
 *  It is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.common.icon;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.TreeSet;

import org.freeplane.core.addins.NodeHookDescriptor;
import org.freeplane.core.addins.PersistentNodeHook;
import org.freeplane.core.controller.Controller;
import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IReadCompletionListener;
import org.freeplane.core.ui.ActionLocationDescriptor;
import org.freeplane.features.common.map.IMapChangeListener;
import org.freeplane.features.common.map.INodeChangeListener;
import org.freeplane.features.common.map.MapChangeEvent;
import org.freeplane.features.common.map.MapModel;
import org.freeplane.features.common.map.ModeController;
import org.freeplane.features.common.map.NodeChangeEvent;
import org.freeplane.features.common.map.NodeModel;
import org.freeplane.features.common.styles.LogicalStyleModel;
import org.freeplane.features.common.styles.MapStyle;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * @author Foltin
 */
@NodeHookDescriptor(hookName = "accessories/plugins/HierarchicalIcons.properties")
@ActionLocationDescriptor(locations = { "/menu_bar/format/nodes/automaticLayout2" })
public class HierarchicalIcons extends PersistentNodeHook implements INodeChangeListener, IMapChangeListener,
        IReadCompletionListener, IExtension {
	public static final String ICONS = "hierarchical_icons";

	public HierarchicalIcons() {
		super();
		final ModeController modeController = Controller.getCurrentModeController();
		modeController.getMapController().getReadManager().addReadCompletionListener(this);
		modeController.getMapController().addNodeChangeListener(this);
		modeController.getMapController().addMapChangeListener(this);
	}

	@Override
	protected void add(final NodeModel node, final IExtension extension) {
		gatherLeavesAndSetStyle(node);
		gatherLeavesAndSetParentsStyle(node);
		super.add(node, extension);
	}

	/**
	 */
	private void addAccumulatedIconsToTreeSet(final NodeModel child, final TreeSet<UIIcon> iconSet) {
		for (final UIIcon icon : IconController.getController().getIcons(child)) {
			iconSet.add(icon);
		}
		final UIIconSet uiIcon = (UIIconSet) child.getStateIcons().get(getHookName());
		if (uiIcon == null) {
			return;
		}
		for (final UIIcon icon : uiIcon.getIcons()) {
			iconSet.add(icon);
		}
	}

	@Override
	protected IExtension createExtension(final NodeModel node, final XMLElement element) {
		return this;
	}

	/**
	 */
	private void gatherLeavesAndSetParentsStyle(final NodeModel node) {
		if (node.getChildCount() == 0) {
			if (node.getParentNode() != null) {
				setStyleRecursive(node.getParentNode());
			}
			return;
		}
		final ListIterator<NodeModel> childrenUnfolded = Controller.getCurrentModeController().getMapController().childrenUnfolded(node);
		while (childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			gatherLeavesAndSetParentsStyle(child);
		}
	}

	/**
	 */
	private void gatherLeavesAndSetStyle(final NodeModel node) {
		if (node.getChildCount() == 0) {
			setStyle(node);
			return;
		}
		final ListIterator<NodeModel> childrenUnfolded = Controller.getCurrentModeController().getMapController().childrenUnfolded(node);
		while (childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			gatherLeavesAndSetStyle(child);
		}
	}

	public void mapChanged(final MapChangeEvent event) {
		final MapModel map = event.getMap();
		if(map == null){
			return;
		}
		final NodeModel rootNode = map.getRootNode();
		if (!isActive(rootNode)) {
			return;
		}
		final Object property = event.getProperty();
		if(! property.equals(MapStyle.MAP_STYLES)){
			return;
		}
		gatherLeavesAndSetStyle(rootNode);
		gatherLeavesAndSetParentsStyle(rootNode);
	}

	public void nodeChanged(final NodeChangeEvent event) {
		final NodeModel node = event.getNode();
		if (!isActive(node)) {
			return;
		}
		final Object property = event.getProperty();
		if (!(property.equals("icon") || property.equals(LogicalStyleModel.class))) {
			return;
		}
		setStyle(node);
		onUpdateChildren(node);
	}

	public void onNodeDeleted(final NodeModel parent, final NodeModel child, final int index) {
		if (!isActive(parent)) {
			return;
		}
		setStyleRecursive(parent);
	}

	public void onNodeInserted(final NodeModel parent, final NodeModel child, final int newIndex) {
		if (!isActive(parent)) {
			return;
		}
		setStyleRecursive(child);
	}

	public void onNodeMoved(final NodeModel oldParent, final int oldIndex, final NodeModel newParent,
	                        final NodeModel child, final int newIndex) {
		if (!isActive(newParent)) {
			return;
		}
		setStyleRecursive(oldParent);
		setStyleRecursive(child);
	}

	public void onPreNodeDelete(final NodeModel parent, final NodeModel child, final int index) {
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * freeplane.extensions.PermanentNodeHook#onUpdateChildrenHook(freeplane.modes
	 * .MindMapNode)
	 */
	private void onUpdateChildren(final NodeModel updatedNode) {
		setStyleRecursive(updatedNode);
	}

	public void readingCompleted(final NodeModel topNode, final HashMap<String, String> newIds) {
		if (!topNode.containsExtension(getClass()) && !topNode.getMap().getRootNode().containsExtension(getClass())) {
			return;
		}
		gatherLeavesAndSetStyle(topNode);
		gatherLeavesAndSetParentsStyle(topNode);
	}

	@Override
	protected void remove(final NodeModel node, final IExtension extension) {
		removeIcons(node);
		super.remove(node, extension);
	}

	/**
	 */
	private void removeIcons(final NodeModel node) {
		node.removeStateIcons(getHookName());
		Controller.getCurrentModeController().getMapController().nodeRefresh(node);
		final ListIterator<NodeModel> childrenUnfolded = Controller.getCurrentModeController().getMapController().childrenUnfolded(node);
		while (childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			removeIcons(child);
		}
	}

	private void setStyle(final NodeModel node) {
		final TreeSet<UIIcon> iconSet = new TreeSet<UIIcon>();
		final ListIterator<NodeModel> childrenUnfolded = Controller.getCurrentModeController().getMapController().childrenUnfolded(node);
		while (childrenUnfolded.hasNext()) {
			final NodeModel child = childrenUnfolded.next();
			addAccumulatedIconsToTreeSet(child, iconSet);
		}
		for (final MindIcon icon : IconController.getController().getIcons(node)) {
			iconSet.remove(icon);
		}
		if (iconSet.size() > 0) {
			node.setStateIcon(getHookName(), new UIIconSet(iconSet, 0.75f), false);
		}
		else {
			node.removeStateIcons(getHookName());
		}
		Controller.getCurrentModeController().getMapController().delayedNodeRefresh(node, HierarchicalIcons.ICONS, null, null);
	}

	/**
	 */
	private void setStyleRecursive(final NodeModel node) {
		setStyle(node);
		if (node.getParentNode() != null) {
			setStyleRecursive(node.getParentNode());
		}
	}

	public void onPreNodeMoved(final NodeModel oldParent, final int oldIndex, final NodeModel newParent,
	                           final NodeModel child, final int newIndex) {
	}
}
