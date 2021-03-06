package com.sirioitalia.api.repository;

import com.sirioitalia.api.model.Color;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ColorRepository extends CrudRepository<Color, Long> {

}
