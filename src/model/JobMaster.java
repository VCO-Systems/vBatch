package model;

import java.io.Serializable;
import javax.persistence.*;
import java.util.List;


/**
 * The persistent class for the JOB_MASTER database table.
 * 
 */
@Entity
@Table(name="JOB_MASTER")
@NamedQuery(name="JobMaster.findAll", query="SELECT j FROM JobMaster j")
public class JobMaster implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(nullable=false, length=100)
	private String description;

	@Column(nullable=false, length=50)
	private String name;

	//bi-directional many-to-one association to JobStepsXref
	@OneToMany(mappedBy="jobMaster")
	private List<JobStepsXref> jobStepsXrefs;

	//bi-directional many-to-one association to VbatchLog
	@OneToMany(mappedBy="jobMaster")
	private List<VbatchLog> vbatchLogs;

	public JobMaster() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<JobStepsXref> getJobStepsXrefs() {
		return this.jobStepsXrefs;
	}

	public void setJobStepsXrefs(List<JobStepsXref> jobStepsXrefs) {
		this.jobStepsXrefs = jobStepsXrefs;
	}

	public JobStepsXref addJobStepsXref(JobStepsXref jobStepsXref) {
		getJobStepsXrefs().add(jobStepsXref);
		jobStepsXref.setJobMaster(this);

		return jobStepsXref;
	}

	public JobStepsXref removeJobStepsXref(JobStepsXref jobStepsXref) {
		getJobStepsXrefs().remove(jobStepsXref);
		jobStepsXref.setJobMaster(null);

		return jobStepsXref;
	}

	public List<VbatchLog> getVbatchLogs() {
		return this.vbatchLogs;
	}

	public void setVbatchLogs(List<VbatchLog> vbatchLogs) {
		this.vbatchLogs = vbatchLogs;
	}

	public VbatchLog addVbatchLog(VbatchLog vbatchLog) {
		getVbatchLogs().add(vbatchLog);
		vbatchLog.setJobMaster(this);

		return vbatchLog;
	}

	public VbatchLog removeVbatchLog(VbatchLog vbatchLog) {
		getVbatchLogs().remove(vbatchLog);
		vbatchLog.setJobMaster(null);

		return vbatchLog;
	}

}