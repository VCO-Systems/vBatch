package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;


/**
 * The persistent class for the JOB_STEPS_XREF database table.
 * 
 */
@Entity
@Table(name="JOB_STEPS_XREF")
@NamedQuery(name="JobStepsXref.findAll", query="SELECT j FROM JobStepsXref j")
public class JobStepsXref implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(nullable=false, length=100)
	private String description;

	@Column(name="JOB_STEP_SEQ", nullable=false)
	private BigDecimal jobStepSeq;

	//bi-directional many-to-one association to JobMaster
	@ManyToOne
	@JoinColumn(name="JOB_ID_ID", nullable=false)
	private JobMaster jobMaster;

	//bi-directional many-to-one association to Step
	@ManyToOne
	@JoinColumn(name="STEP_ID", nullable=false)
	private Step step;

	public JobStepsXref() {
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

	public BigDecimal getJobStepSeq() {
		return this.jobStepSeq;
	}

	public void setJobStepSeq(BigDecimal jobStepSeq) {
		this.jobStepSeq = jobStepSeq;
	}

	public JobMaster getJobMaster() {
		return this.jobMaster;
	}

	public void setJobMaster(JobMaster jobMaster) {
		this.jobMaster = jobMaster;
	}

	public Step getStep() {
		return this.step;
	}

	public void setStep(Step step) {
		this.step = step;
	}

}