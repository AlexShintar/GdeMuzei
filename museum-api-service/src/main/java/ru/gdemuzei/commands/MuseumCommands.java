package ru.gdemuzei.commands;

import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.BorderStyle;
import org.springframework.shell.table.TableBuilder;
import org.springframework.shell.table.TableModelBuilder;
import ru.gdemuzei.dto.MuseumSummaryDto;
import ru.gdemuzei.services.MuseumService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
@ShellComponent
//@Profile("shell")
public class MuseumCommands {

    private final MuseumService museumService;

    /**
     * К удалению, использовал для вывода списка пока не было сервиса админа
     */
    @ShellMethod(value = "Find all museums", key = "am")
    public String findAllMuseumsForAdminAsync(
            @ShellOption(defaultValue = "0", help = "Page number to retrieve") int page,
            @ShellOption(defaultValue = "10", help = "Number of items per page") int size
    ) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> resultHolder = new AtomicReference<>();
        final AtomicReference<Throwable> errorHolder = new AtomicReference<>();

        museumService.findAllForAdmin(PageRequest.of(page, size))
                .subscribe(
                        pageResult -> {
                            if (pageResult == null || pageResult.getContent().isEmpty()) {
                                resultHolder.set("No museums found for this page.");
                                latch.countDown();
                                return;
                            }

                            TableModelBuilder<Object> modelBuilder = new TableModelBuilder<>();
                            modelBuilder.addRow()
                                    .addValue(" ID ")
                                    .addValue(" Name ")
                                    .addValue(" Locality ")
                                    .addValue(" Verified ");

                            for (MuseumSummaryDto museum : pageResult.getContent()) {
                                modelBuilder.addRow()
                                        .addValue(museum.id())
                                        .addValue(museum.name())
                                        .addValue(museum.locality())
                                        .addValue(museum.verified());
                            }

                            TableBuilder tableBuilder = new TableBuilder(modelBuilder.build());
                            tableBuilder.addFullBorder(BorderStyle.oldschool);

                            String table = tableBuilder.build().render(150);

                            String paginationInfo = String.format(
                                    "Page %d of %d. Total museums: %d",
                                    pageResult.getNumber(), pageResult.getTotalPages(), pageResult.getTotalElements()
                            );

                            String nextCommandHint = "";
                            if (!pageResult.isLast()) {
                                nextCommandHint = String.format(
                                        "\nTo view the next page, run: am --page %d --size %d",
                                        page + 1, size
                                );
                            }

                            resultHolder.set(table + "\n" + paginationInfo + nextCommandHint);
                        },
                        error -> {
                            errorHolder.set(error);
                            latch.countDown();
                        },
                        latch::countDown
                );

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Command was interrupted.";
        }

        if (errorHolder.get() != null) {
            return "An error occurred: " + errorHolder.get().getMessage();
        }

        return resultHolder.get();
    }
}
