package com.vattima.lego.data.service.controller;

import lombok.RequiredArgsConstructor;
import net.lego.data.v2.dao.ItemInventoryDao;
import net.lego.data.v2.dto.ItemInventory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/items/inventory")
@RequiredArgsConstructor
public class ItemInventoryController {

    private final ItemInventoryDao itemInventoryDao;

    @GetMapping
    public ResponseEntity<List<ItemInventory>> findAll() {
        return ResponseEntity.ok(itemInventoryDao.findAll());
    }
}
