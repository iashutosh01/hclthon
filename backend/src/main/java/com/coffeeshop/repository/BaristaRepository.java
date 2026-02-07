package com.coffeeshop.repository;

import com.coffeeshop.model.Barista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BaristaRepository extends JpaRepository<Barista, Long> {

    List<Barista> findAllByOrderByIdAsc();

    Barista findByName(String name);
}
