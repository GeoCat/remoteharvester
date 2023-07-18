package geocat.service;

import geocat.database.entities.HarvestJob;
import geocat.database.entities.HarvestJobState;
import geocat.database.repos.HarvestJobRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class DeleteJobService {

    private static final Logger logger = LoggerFactory.getLogger(DeleteJobService.class);


    @Autowired
    LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    HarvestJobRepo harvestJobRepo;

    public void executeSql(List<String> sqls, String jobId) {
        EntityManager entityManager = null;
        try {
            entityManager = localContainerEntityManagerFactoryBean.createNativeEntityManager(null);
            entityManager.getTransaction().begin();
            for (String sql : sqls) {
                Query q = entityManager.createNativeQuery(sql);
                q.setParameter(1, jobId);
                int n = q.executeUpdate();
                logger.trace(jobId + " - " + n + " - " + sql);
            }
            entityManager.getTransaction().commit();
        } finally {
            if (entityManager != null) {
                if (entityManager.getTransaction() != null) {
                    try {
                        entityManager.getTransaction().rollback();
                    } catch (Exception e) {
                        // do nothing
                    }
                }
                entityManager.close();
            }
        }
    }

    static List<String> deleteSQLS = Arrays.asList(new String[]{
            "DELETE FROM metadata_record WHERE endpoint_job_id IN (SELECT endpoint_job_id FROM endpoint_job WHERE harvest_job_id = ?)",
            "DELETE FROM record_set WHERE endpoint_job_id IN (SELECT endpoint_job_id FROM endpoint_job WHERE harvest_job_id = ?)",

            "DELETE FROM  logging_event WHERE jms_correlation_id = ? AND reference_flag NOT IN (2,3)",
            "DELETE FROM  logging_event_exception WHERE event_id IN (SELECT event_id FROM logging_event WHERE jms_correlation_id = ?)",
            "DELETE FROM  logging_event WHERE jms_correlation_id = ?",

            "DELETE FROM endpoint_job WHERE harvest_job_id = ?",
            "DELETE FROM harvest_job WHERE  job_id = ?",
    });

    public String deleteById(String jobId) throws Exception {
        if (jobId == null)
            throw new Exception("delete - jobid is null");
        jobId = jobId.trim();
        Optional<HarvestJob> _job = harvestJobRepo.findById(jobId);
        if (!_job.isPresent())
            throw new Exception("couldnt find that job id = " + jobId);
        HarvestJob job = _job.get();

        jobId = job.getJobId(); // this prevents sql injection
        if (jobId.contains("'") || jobId.contains("\\"))
            throw new Exception("bad   job"); // this shouldn't be possible

        executeSql(deleteSQLS, jobId);

        return "DELETED";
    }

    public void insertToDBImmediate(HarvestJob job) {

    }


    public String ensureAtMost(String longTermTag, int maxAllowed, String jobIdDoNotDelete) throws Exception {
        if (longTermTag == null)
            throw new Exception("delete - countryCode is null");
        longTermTag = longTermTag.trim();

        List<HarvestJob> jobs = harvestJobRepo.findByLongTermTag(longTermTag);
        // don't delete the one being created...
        jobs = jobs.stream()
                .filter(x -> !x.getJobId().equals(jobIdDoNotDelete))
                .collect(Collectors.toList());

        if (jobs.size() <= maxAllowed)
            return "Nothing to do - job count=" + jobs.size();

        Collections.sort(jobs,
                Comparator.comparing(HarvestJob::getCreateTimeUTC));
        Collections.reverse(jobs);

        //preferentially remove ERROR and USERABORT jobs (they aren't worth keeping)
        List<HarvestJob> jobsToDelete = jobs.stream()
                .filter(x -> x.getState() == HarvestJobState.USERABORT || x.getState() == HarvestJobState.ERROR)
                .collect(Collectors.toList());

        jobs.removeAll(jobsToDelete);

        if (jobs.size() > maxAllowed)
            jobsToDelete.addAll(jobs.subList(maxAllowed, jobs.size()));

        String result = jobsToDelete.size() + " jobs were deleted.";
        for (HarvestJob job : jobsToDelete) {
            logger.debug("ensureAtMost: deleting jobid=" + job.getJobId());
            deleteById(job.getJobId());
            result += job.getJobId() + ",";
        }
        return result;

    }
}
