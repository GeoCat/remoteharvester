package com.geocat.ingester.dao.harvester;

import com.geocat.ingester.model.harvester.BlobStorage;
import org.springframework.data.repository.CrudRepository;

public interface BlogStorageRepo extends CrudRepository<BlobStorage, String> {
}
