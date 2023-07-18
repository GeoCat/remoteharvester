package geocat.integrationtests;

import geocat.database.entities.HarvestJob;
import geocat.database.entities.HarvestJobState;
import geocat.database.repos.HarvestJobRepo;
import geocat.database.service.HarvestJobService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.Assert.assertSame;


//Access the database by;
// http://localhost:8080/h2
// login (see application-integrationtest.properties)
// db:   jdbc:h2:mem:testdb
// user: sa
// pass: password
//
@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
        //  classes = MySpringApp.class
)
@TestPropertySource(
        locations = "classpath:application-integrationtest.properties")
public class SimpleDBTests {

//    @Autowired
//    private TestEntityManager entityManager;

    @Autowired
    private HarvestJobRepo harvestJobRepo;

    @Autowired
    private HarvestJobService harvestJobService;

    @Test
    public void testDB() throws Exception {
        HarvestJob newJob = new HarvestJob();
        newJob.setJobId((UUID.randomUUID().toString()));
        // newJob.setFilter(event.getFilter());
        newJob.setLookForNestedDiscoveryService(false);
        newJob.setInitialUrl("http://hi.com");
        newJob.setLongTermTag("testcase1");
        newJob.setState(HarvestJobState.CREATING);
        // newJob.setProblematicResultsConfigurationJSON(event.getProblematicResultsConfigurationJSON());
        newJob.setNrecordsPerRequest(10);
        // newJob.setGetRecordQueueHint(event.getGetRecordQueueHint());
        HarvestJob newJob2 = harvestJobRepo.save(newJob);
        harvestJobService.updateHarvestJobStateInDBToError(newJob2.getJobId());
        HarvestJob newJob3 = harvestJobRepo.findById(newJob2.getJobId()).get();
        assertSame(HarvestJobState.ERROR, newJob3.getState());
        Thread.sleep(100 * 1000); // access database
    }

}
