package org.freeplane.core.ui.menubuilders.menu;

import org.freeplane.core.ui.menubuilders.action.ActionFinder;
import org.freeplane.core.ui.menubuilders.action.ActionSelectListener;
import org.freeplane.core.ui.menubuilders.action.ResourceAccessor;
import org.freeplane.core.ui.menubuilders.generic.BuilderDestroyerPair;
import org.freeplane.core.ui.menubuilders.generic.ChildProcessor;
import org.freeplane.core.ui.menubuilders.generic.EntryPopupListenerCollection;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.core.ui.menubuilders.generic.PhaseProcessor;
import org.freeplane.core.ui.menubuilders.generic.RecursiveMenuStructureProcessor;
import org.freeplane.features.mode.FreeplaneActions;

public class MenuBuildProcessFactory {

	public PhaseProcessor createBuildProcessor(FreeplaneActions freeplaneActions, ResourceAccessor menuEntryBuilder) {
		RecursiveMenuStructureProcessor recursiveMenuStructureBuilder = new RecursiveMenuStructureProcessor();

		recursiveMenuStructureBuilder.setDefaultBuilder(EntryVisitor.EMTPY_VISITOR);

		recursiveMenuStructureBuilder.addBuilderPair("toolbar", new BuilderDestroyerPair(new JToolbarBuilder()));
		recursiveMenuStructureBuilder.setSubtreeDefaultBuilderPair("toolbar", "toolbar.action");
		recursiveMenuStructureBuilder.addBuilderPair("toolbar.action", new BuilderDestroyerPair(
		    new JToolbarActionBuilder()));

		recursiveMenuStructureBuilder.addBuilderPair("main_menu", new BuilderDestroyerPair(new JMenubarBuilder()));
		recursiveMenuStructureBuilder.setSubtreeDefaultBuilderPair("main_menu", "menu.action");
		final ChildProcessor childBuilder = new ChildProcessor();
		final ActionSelectListener actionSelectListener = new ActionSelectListener();
		EntryPopupListenerCollection entryPopupListenerCollection = new EntryPopupListenerCollection();
		entryPopupListenerCollection.addEntryPopupListener(childBuilder);
		entryPopupListenerCollection.addEntryPopupListener(actionSelectListener);
		recursiveMenuStructureBuilder.addBuilderPair("menu.action", new BuilderDestroyerPair(new JMenuItemBuilder(
		    entryPopupListenerCollection, menuEntryBuilder), new JComponentRemover()));

		final RecursiveMenuStructureProcessor actionBuilder = new RecursiveMenuStructureProcessor();
		
		actionBuilder.setDefaultBuilder(new ActionFinder(freeplaneActions ));
		final PhaseProcessor buildProcessor = new PhaseProcessor(actionBuilder,recursiveMenuStructureBuilder);
		childBuilder.setProcessor(buildProcessor);
		return buildProcessor;
	}
}

