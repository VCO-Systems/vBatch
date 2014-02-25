package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;


/**
 * The persistent class for the JOB_DEFINITION database table.
 * 
 */
@Entity
@Table(name="JOB_DEFINITION")
@NamedQuery(name="JobDefinition.findAll", query="SELECT j FROM JobDefinition j")
public class JobDefinition implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(name="LONG_DESC", length=150)
	private String longDesc;

	@Column(name="ORDER_NUM", nullable=false)
	private BigDecimal orderNum;

	@Column(name="SHORT_DESC", length=20)
	private String shortDesc;

	//bi-directional many-to-one association to BatchLog
	@OneToMany(mappedBy="jobDefinition")
	private List<BatchLog> batchLogs;

	//bi-directional many-to-one association to JobStepsXref
	@OneToMany(mappedBy="jobDefinition")
	private List<JobStepsXref> jobStepsXrefs;

	public JobDefinition() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getLongDesc() {
		return this.longDesc;
	}

	public void setLongDesc(String longDesc) {
		this.longDesc = longDesc;
	}

	public BigDecimal getOrderNum() {
		return this.orderNum;
	}

	public void setOrderNum(BigDecimal orderNum) {
		this.orderNum = orderNum;
	}

	public String getShortDesc() {
		return this.shortDesc;
	}

	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	public List<BatchLog> getBatchLogs() {
		return this.batchLogs;
	}

	public void setBatchLogs(List<BatchLog> batchLogs) {
		this.batchLogs = batchLogs;
	}

	public BatchLog addBatchLog(BatchLog batchLog) {
		getBatchLogs().add(batchLog);
		batchLog.setJobDefinition(this);

		return batchLog;
	}

	public BatchLog removeBatchLog(BatchLog batchLog) {
		getBatchLogs().remove(batchLog);
		batchLog.setJobDefinition(null);

		return batchLog;
	}

	public List<JobStepsXref> getJobStepsXrefs() {
		return this.jobStepsXrefs;
	}

	public void setJobStepsXrefs(List<JobStepsXref> jobStepsXrefs) {
		this.jobStepsXrefs = jobStepsXrefs;
	}

	public JobStepsXref addJobStepsXref(JobStepsXref jobStepsXref) {
		getJobStepsXrefs().add(jobStepsXref);
		jobStepsXref.setJobDefinition(this);

		return jobStepsXref;
	}

	public JobStepsXref removeJobStepsXref(JobStepsXref jobStepsXref) {
		getJobStepsXrefs().remove(jobStepsXref);
		jobStepsXref.setJobDefinition(null);

		return jobStepsXref;
	}

}