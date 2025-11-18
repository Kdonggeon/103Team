// 새 파일: DirectorAcademyController.java
package com.team103.controller;

import com.team103.model.Director;
import com.team103.repository.DirectorRepository;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/directors/academies")
public class DirectorAcademyController {

    private final DirectorRepository directorRepository;

    public DirectorAcademyController(DirectorRepository directorRepository) {
        this.directorRepository = directorRepository;
    }

    @DeleteMapping("/{academyNumber}")
    public ResponseEntity<?> deleteAcademyFromDirector(
            @PathVariable int academyNumber,
            Authentication auth
    ) {
      if (auth == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "UNAUTHENTICATED"));
      }

      String username = auth.getName();
      Director me = directorRepository.findByUsername(username);
      if (me == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "DIRECTOR_NOT_FOUND"));
      }

      if (me.getAcademyNumbers() == null || me.getAcademyNumbers().isEmpty()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "NO_ACADEMY_REGISTERED"));
      }

      boolean existed = me.getAcademyNumbers().removeIf(n -> n == academyNumber);
      if (!existed) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "ACADEMY_NOT_IN_LIST"));
      }

      directorRepository.save(me);

      return ResponseEntity.ok(Map.of(
              "status", "ok",
              "deleted", academyNumber
      ));
    }
}
