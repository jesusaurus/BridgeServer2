package org.sagebionetworks.bridge.hibernate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sagebionetworks.bridge.models.SearchTermPredicate.AND;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.sagebionetworks.bridge.dao.AdherenceReportDao;
import org.sagebionetworks.bridge.hibernate.QueryBuilder.WhereClauseBuilder;
import org.sagebionetworks.bridge.models.AccountTestFilter;
import org.sagebionetworks.bridge.models.PagedResourceList;
import org.sagebionetworks.bridge.models.schedules2.adherence.weekly.WeeklyAdherenceReport;
import org.springframework.stereotype.Component;

import com.google.common.base.Joiner;

@Component
public class HibernateAdherenceReportDao implements AdherenceReportDao {

    static final String COMPLIANCE_UNDER_FIELD = "complianceUnder";
    static final String LABEL_FILTER_FIELD = "labelFilter";
    static final String STUDY_ID_FIELD = "studyId";
    static final String APP_ID_FIELD = "appId";
    
    private HibernateHelper hibernateHelper;

    @Resource(name = "mysqlHibernateHelper")
    final void setHibernateHelper(HibernateHelper hibernateHelper) {
        this.hibernateHelper = hibernateHelper;
    }

    @Override
    public void saveWeeklyAdherenceReport(WeeklyAdherenceReport report) {
        checkNotNull(report);
        
        hibernateHelper.saveOrUpdate(report);
    }

    @Override
    public PagedResourceList<WeeklyAdherenceReport> getWeeklyAdherenceReports(String appId, String studyId,
            AccountTestFilter testFilter, List<String> labelFilter, Integer complianceUnder, Integer offsetBy,
            Integer pageSize) {
        checkNotNull(appId);
        checkNotNull(studyId);
        checkNotNull(testFilter);
        
        QueryBuilder builder = new QueryBuilder();
        builder.append("FROM WeeklyAdherenceReport h");
        if (labelFilter != null) {
            builder.append("JOIN h.labels label");    
        }
        WhereClauseBuilder where = builder.startWhere(AND);
        where.append("h.appId = :appId", APP_ID_FIELD, appId);
        where.append("h.studyId = :studyId", STUDY_ID_FIELD, studyId);
        if (complianceUnder != null) {
            where.append("weeklyAdherencePercent < :complianceUnder", COMPLIANCE_UNDER_FIELD, complianceUnder);
        }
        if (labelFilter != null && !labelFilter.isEmpty()) {
            List<String> phrases = new ArrayList<>();
            for (int i=0; i < labelFilter.size(); i++) {
                phrases.add("label LIKE :labelFilter"+i);
                where.getParameters().put("labelFilter"+i,  "%" + labelFilter.get(i) + "%");
            }
            where.append("(" + Joiner.on(" OR ").join(phrases) + ")");
        }
        if (testFilter == AccountTestFilter.TEST) {
            where.append("testAccount = 1");
        } else if (testFilter == AccountTestFilter.PRODUCTION) {
            where.append("testAccount = 0");
        }
        builder.append("ORDER BY weeklyAdherencePercent");
        
        int total = hibernateHelper.queryCount("SELECT COUNT(*) " + builder.getQuery(), builder.getParameters());
        
        List<WeeklyAdherenceReport> reports = hibernateHelper.queryGet("SELECT h " + builder.getQuery(),
                builder.getParameters(), offsetBy, pageSize, WeeklyAdherenceReport.class);

        return new PagedResourceList<>(reports, total, true);
    }
}    
