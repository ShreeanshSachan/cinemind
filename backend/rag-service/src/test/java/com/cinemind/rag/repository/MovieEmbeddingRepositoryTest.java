package com.cinemind.rag.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.cinemind.rag.entity.MovieEmbedding;
import com.cinemind.rag.util.EmbeddingCodec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class MovieEmbeddingRepositoryTest {

    @Autowired
    private MovieEmbeddingRepository repository;

    @Test
    void persistsAndFindsByMovieId() {
        repository.save(embedding(17L, "doc", new float[]{0.1f, 0.2f}));

        MovieEmbedding found = repository.findByMovieId(17L).orElseThrow();

        assertThat(found.getMovieId()).isEqualTo(17L);
        assertThat(found.getDocumentContent()).isEqualTo("doc");
        assertThat(found.getModelName()).isEqualTo("nomic-embed-text");
        assertThat(EmbeddingCodec.decode(found.getEmbeddingJson()))
                .containsExactly(0.1f, 0.2f);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
        assertThat(repository.existsByMovieId(17L)).isTrue();
        assertThat(repository.existsByMovieId(18L)).isFalse();
    }

    @Test
    void enforcesUniqueMovieId() {
        repository.save(embedding(17L, "doc", new float[]{1f}));
        repository.flush();

        assertThatThrownBy(() -> {
            repository.save(embedding(17L, "duplicate", new float[]{2f}));
            repository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsCandidatesByMovieIdIn() {
        repository.save(embedding(1L, "one", new float[]{1f}));
        repository.save(embedding(2L, "two", new float[]{2f}));
        repository.save(embedding(3L, "three", new float[]{3f}));

        List<MovieEmbedding> found = repository.findAllByMovieIdIn(List.of(1L, 3L));

        assertThat(found).extracting(MovieEmbedding::getMovieId)
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    void updatesExistingRowInsteadOfCreatingADuplicate() {
        MovieEmbedding existing = repository.save(embedding(17L, "old", new float[]{1f}));
        LocalDateTime createdAt = existing.getCreatedAt();
        assertThat(createdAt).isNotNull();

        existing.setDocumentContent("new");
        existing.setEmbeddingJson(EmbeddingCodec.encode(new float[]{9f}));
        repository.save(existing);
        repository.flush();

        assertThat(repository.count()).isEqualTo(1);
        MovieEmbedding updated = repository.findByMovieId(17L).orElseThrow();
        assertThat(updated.getDocumentContent()).isEqualTo("new");
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
    }

    private static MovieEmbedding embedding(long movieId, String document, float[] vector) {
        MovieEmbedding entity = new MovieEmbedding();
        entity.setMovieId(movieId);
        entity.setDocumentContent(document);
        entity.setEmbeddingJson(EmbeddingCodec.encode(vector));
        entity.setModelName("nomic-embed-text");
        return entity;
    }
}
