package org.c4sg.dao;

import java.util.List;

import org.c4sg.entity.Bookmark;
import org.springframework.data.repository.CrudRepository;

public interface BookmarkDAO extends CrudRepository<Bookmark, Long> {

	List<Bookmark> findByUser_IdAndProject_Id(Integer userId, Integer projectId);
}