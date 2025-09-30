package ru.gdemuzei.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.gdemuzei.client.MuseumApiClient;
import ru.gdemuzei.dto.MuseumCreateRequest;
import ru.gdemuzei.dto.MuseumSummaryDto;
import ru.gdemuzei.dto.MuseumUpdateRequest;

import static org.springframework.validation.BindingResult.MODEL_KEY_PREFIX;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MuseumViewController {

    private final MuseumApiClient museumApiClient;

    /**
     * Отображает страницу со списком музеев с пагинацией.
     *
     * @param page  номер страницы (по умолчанию 0).
     * @param size  количество элементов на странице (по умолчанию 20).
     * @param model модель для передачи данных в шаблон.
     * @return имя шаблона "museums-list".
     */
    @GetMapping("/museums")
    public String showMuseumsPage(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  Model model) {
        Mono<Page<MuseumSummaryDto>> museumsPageMono = museumApiClient.getMuseumsPaginated(page, size);
        model.addAttribute("museumsPage", museumsPageMono);
        return "museums-list";
    }

    /**
     * Отображает страницу редактирования для существующего музея.
     *
     * @param id       идентификатор музея.
     * @param fromPage страница, с которой был выполнен переход (для возврата).
     * @param fromSize размер страницы, с которой был выполнен переход.
     * @param model    модель для передачи данных в шаблон.
     * @return асинхронный результат с именем шаблона "museum-edit".
     */
    @GetMapping("/museums/{id}")
    public Mono<String> showMuseumEditPage(@PathVariable String id,
                                           @RequestParam(name = "fromPage", defaultValue = "0") int fromPage,
                                           @RequestParam(name = "fromSize", defaultValue = "20") int fromSize,
                                           Model model) {
        return museumApiClient.getMuseumById(id)
                .flatMap(museum -> {
                    model.addAttribute("museum", museum);
                    model.addAttribute("fromPage", fromPage);
                    model.addAttribute("fromSize", fromSize);
                    return Mono.just("museum-edit");
                })
                .switchIfEmpty(Mono.just("museum-not-found"));
    }

    /**
     * Отображает пустую форму для создания нового музея.
     *
     * @param model модель для передачи данных в шаблон.
     * @return имя шаблона "museum-new".
     */
    @GetMapping("/museums/create")
    public String showCreateMuseumForm(Model model) {
        if (!model.containsAttribute("museum")) {
            model.addAttribute("museum", MuseumCreateRequest.empty());
        }
        return "museum-new";
    }

    /**
     * Обрабатывает POST-запрос на создание нового музея.
     * При ошибках валидации возвращает пользователя на форму создания с сообщениями об ошибках.
     *
     * @param form объект DTO с данными для создания музея.
     * @param br   результат валидации.
     * @return асинхронный результат рендеринга.
     */
    @PostMapping("/museums")
    public Mono<Rendering> createMuseum(@Valid @ModelAttribute("museum") MuseumCreateRequest form,
                                        BindingResult br) {
        if (br.hasErrors()) {
            return Mono.just(Rendering.view("museum-new")
                    .modelAttribute("museum", form)
                    .modelAttribute(MODEL_KEY_PREFIX + "museum", br)
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build());
        }
        return museumApiClient.createMuseum(form)
                .map(created -> Rendering.redirectTo("/museums").build());
    }

    /**
     * Обрабатывает POST-запрос на обновление существующего музея.
     * При ошибках валидации возвращает пользователя на форму редактирования с сообщениями об ошибках.
     *
     * @param id   идентификатор обновляемого музея.
     * @param form объект DTO с данными для обновления.
     * @param br   результат валидации.
     * @return асинхронный результат рендеринга.
     */
    @PostMapping("/museums/{id}")
    public Mono<Rendering> updateMuseum(@PathVariable String id,
                                        @Valid @ModelAttribute("museum") MuseumUpdateRequest form,
                                        BindingResult br,
                                        @RequestParam(name = "fromPage", defaultValue = "0") int fromPage,
                                        @RequestParam(name = "fromSize", defaultValue = "20") int fromSize) {
        if (br.hasErrors()) {
            return Mono.just(Rendering.view("museum-edit")
                    .modelAttribute("museum", form)
                    .modelAttribute(MODEL_KEY_PREFIX + "museum", br)
                    .modelAttribute("fromPage", fromPage)
                    .modelAttribute("fromSize", fromSize)
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build());
        }
        return museumApiClient.updateMuseum(id, form)
                .thenReturn(Rendering.redirectTo("/museums?page=" + fromPage + "&size=" + fromSize).build());
    }
}
