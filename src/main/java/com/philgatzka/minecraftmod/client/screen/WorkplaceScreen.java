package com.philgatzka.minecraftmod.client.screen;

import com.philgatzka.minecraftmod.client.ui.UiButton;
import com.philgatzka.minecraftmod.client.ui.UiColumn;
import com.philgatzka.minecraftmod.client.ui.UiScreen;
import com.philgatzka.minecraftmod.client.ui.UiSelectionList;
import com.philgatzka.minecraftmod.client.ui.UiTheme;
import com.philgatzka.minecraftmod.menu.WorkplaceInfo;
import com.philgatzka.minecraftmod.menu.WorkplaceInfo.SettlerEntry;
import com.philgatzka.minecraftmod.menu.WorkplaceMenu;
import com.philgatzka.minecraftmod.network.AssignWorkerPacket;
import com.philgatzka.minecraftmod.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

// Lets the settlement's leader pick which settler works at this workplace (e.g. who is the Builder's Hut's builder)
public class WorkplaceScreen extends UiScreen<WorkplaceMenu>
{
    private static final int LIST_ROWS = 6;

    @Nullable
    private UiSelectionList<SettlerEntry> settlerList;
    @Nullable
    private UiButton assignButton;
    @Nullable
    private UiButton unassignButton;
    // Survives init() on window resize
    @Nullable
    private UUID selectedId;

    public WorkplaceScreen(WorkplaceMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title, 220, 196);
    }

    @Override
    protected void init()
    {
        super.init();
        settlerList = null;
        assignButton = null;
        unassignButton = null;

        WorkplaceInfo info = menu.getInfo();
        Component jobName = info.job().displayName();
        Component workerName = info.workerName() == null ? Component.translatable("gui.minecraftmod.workplace.nobody") : Component.literal(info.workerName());
        UiColumn column = column();

        if (info.settlementName() == null)
        {
            column.label(Component.translatable("gui.minecraftmod.workplace.unclaimed"), UiTheme.TEXT);
            return;
        }
        column.label(Component.translatable("gui.minecraftmod.workplace.settlement", info.settlementName()), UiTheme.TEXT_MUTED);
        column.label(Component.translatable("gui.minecraftmod.workplace.worker", jobName, workerName), UiTheme.TEXT);
        if (!info.canManage())
            return;
        if (info.settlers().isEmpty())
        {
            column.label(Component.translatable("gui.minecraftmod.workplace.no_settlers"), UiTheme.TEXT_MUTED);
            return;
        }

        column.space(2);
        column.label(Component.translatable("gui.minecraftmod.workplace.choose", jobName), UiTheme.TEXT_MUTED);
        settlerList = column.selectionList(LIST_ROWS, info.settlers(), entry -> rowText(entry, jobName), entry -> {
            selectedId = entry.id();
            clearError();
        });
        if (selectedId == null)
            info.settlers().stream().filter(SettlerEntry::worksHere).findFirst().ifPresent(entry -> selectedId = entry.id());
        settlerList.select(find(info.settlers(), selectedId));

        List<UiButton> buttons = column.buttonRow(
                new UiColumn.ButtonSpec(Component.translatable("gui.minecraftmod.workplace.assign"), this::assignSelected),
                new UiColumn.ButtonSpec(Component.translatable("gui.minecraftmod.workplace.unassign"), this::unassign));
        assignButton = buttons.get(0);
        unassignButton = buttons.get(1);
        column.errorLabel();
        updateButtons();
    }

    private static Component rowText(SettlerEntry entry, Component jobName)
    {
        if (entry.worksHere())
            return Component.translatable("gui.minecraftmod.workplace.row.here", entry.name(), jobName);
        if (entry.job() != null)
            return Component.translatable("gui.minecraftmod.workplace.row.elsewhere", entry.name(), entry.job().displayName());
        return Component.translatable("gui.minecraftmod.workplace.row.unemployed", entry.name());
    }

    @Nullable
    private static SettlerEntry find(List<SettlerEntry> entries, @Nullable UUID id)
    {
        return entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    protected void containerTick()
    {
        super.containerTick();
        updateButtons();
    }

    private void updateButtons()
    {
        if (settlerList == null || assignButton == null || unassignButton == null)
            return;
        SettlerEntry selected = settlerList.getSelected();
        assignButton.active = selected != null && !selected.worksHere();
        unassignButton.active = menu.getInfo().workerName() != null;
    }

    // The server checks everything again; on success it reopens this screen showing the new assignment
    private void assignSelected()
    {
        SettlerEntry selected = settlerList == null ? null : settlerList.getSelected();
        if (selected == null)
            return;
        clearError();
        ModNetwork.CHANNEL.sendToServer(new AssignWorkerPacket(selected.id()));
    }

    private void unassign()
    {
        clearError();
        ModNetwork.CHANNEL.sendToServer(new AssignWorkerPacket(null));
    }
}
