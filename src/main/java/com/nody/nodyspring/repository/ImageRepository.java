package com.nody.nodyspring.repository;

import com.nody.nodyspring.DB.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Integer> {
    List<Image> findByBoardimg(Integer boardimg);
}
