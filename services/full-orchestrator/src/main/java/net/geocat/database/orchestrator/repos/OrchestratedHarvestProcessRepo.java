package net.geocat.database.orchestrator.repos;

import net.geocat.database.orchestrator.entities.OrchestratedHarvestProcess;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Scope("prototype")
public interface OrchestratedHarvestProcessRepo extends CrudRepository<OrchestratedHarvestProcess, String> {

    @Query("SELECT o FROM OrchestratedHarvestProcess o WHERE  o.state not in (net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState.ERROR, net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState.USERABORT, net.geocat.database.orchestrator.entities.OrchestratedHarvestProcessState.COMPLETE)")
    List<OrchestratedHarvestProcess> findOutstanding();
}