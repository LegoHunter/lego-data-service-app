package com.vattima.lego.data.service.controller;

import lombok.RequiredArgsConstructor;
import net.lego.data.v2.dao.ItemDao;
import net.lego.data.v2.dto.Item;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/api/v1/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemDao itemDao;

    @GetMapping("/")
    public ResponseEntity<List<Item>> findAll() {
        return ResponseEntity.ok(itemDao.findAll());
    }
}
