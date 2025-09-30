package ru.gdemuzei.commands;

import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;
import ru.gdemuzei.dto.MuseumResponse;
import ru.gdemuzei.services.MuseumService;

import java.util.List;

@RequiredArgsConstructor
@ShellComponent
public class MuseumCommands {

    private final MuseumService museumService;

    @ShellMethod(value = "Find all museums", key = "am")
    public String  findAllMuseums() {
        List<MuseumResponse> museums = museumService.findAll().collectList().block();
        if (museums == null || museums.isEmpty()) {
            return "No museums found in the database.";
        }
        TableModelBuilder modelBuilder = new TableModelBuilder();
        modelBuilder.addRow()
                .addValue(" ID ")
                .addValue(" Name ")
                .addValue(" Longitude ")
                .addValue(" Latitude ")
                .addValue(" telegram ");
        for (MuseumResponse museum : museums) {
            modelBuilder.addRow()
                    .addValue(museum.id())
                    .addValue(museum.name())
                    .addValue(String.format("%.6f", museum.longitude()))
                    .addValue(String.format("%.6f", museum.latitude()))
                    .addValue(museum.telegram());
        }
        TableBuilder tableBuilder = new TableBuilder(modelBuilder.build());
        tableBuilder.addFullBorder(BorderStyle.oldschool);
        return tableBuilder.build().render(120);
    }
}