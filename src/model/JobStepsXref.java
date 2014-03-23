package model;

import java.io.Serializable;
import javax.persistence.*;


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

	@Column(name="JOB_STEP_SEQ", nullable=false)
	private Long jobStepSeq;

	@Column(name="SPECIAL_MODE", length=25)
	private String specialMode;

	//bi-directional many-to-one association to JobDefinition
	@ManyToOne
	@JoinColumn(name="JOB_DEFINITION_ID", nullable=false)
	private JobDefinition jobDefinition;

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

	public Long getJobStepSeq() {
		return this.jobStepSeq;
	}

	public void setJobStepSeq(Long jobStepSeq) {
		this.jobStepSeq = jobStepSeq;
	}

	public String getSpecialMode() {
		return this.specialMode;
	}

	public void setSpecialMode(String specialMode) {
		this.specialMode = specialMode;
	}

	public JobDefinition getJobDefinition() {
		return this.jobDefinition;
	}

	public void setJobDefinition(JobDefinition jobDefinition) {
		this.jobDefinition = jobDefinition;
	}

	public Step getStep() {
		return this.step;
	}

	public void setStep(Step step) {
		this.step = step;
	}

}