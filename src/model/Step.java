package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.List;


/**
 * The persistent class for the STEPS database table.
 * 
 */
@Entity
@Table(name="STEPS")
@NamedQuery(name="Step.findAll", query="SELECT s FROM Step s")
public class Step implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(name="CLASS_PATH", length=150)
	private String classPath;

	@Column(name="EXTRACT_COMMIT_FREQ")
	private BigDecimal extractCommitFreq;

	@Column(name="EXTRACT_MAX_REC_PER_FILE")
	private BigDecimal extractMaxRecPerFile;

	@Column(name="EXTRACT_SQL", length=4000)
	private String extractSql;

	@Column(name="LONG_DESC", length=150)
	private String longDesc;

	@Column(name="OUTPUT_FILE_FORMAT", length=10)
	private String outputFileFormat;

	@Column(name="OUTPUT_FILENAME_POSTFIX", length=100)
	private String outputFilenamePostfix;

	@Column(name="OUTPUT_FILENAME_PREFIX", length=100)
	private String outputFilenamePrefix;

	@Column(length=100)
	private String param1;

	@Column(length=50)
	private String param2;

	@Column(length=50)
	private String param3;

	@Column(name="SHORT_DESC", length=20)
	private String shortDesc;

	@Column(name="\"TYPE\"", nullable=false, length=50)
	private String type;

	//bi-directional many-to-one association to JobStepsXref
	@OneToMany(mappedBy="step")
	private List<JobStepsXref> jobStepsXrefs;

	public Step() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getClassPath() {
		return this.classPath;
	}

	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}

	public BigDecimal getExtractCommitFreq() {
		return this.extractCommitFreq;
	}

	public void setExtractCommitFreq(BigDecimal extractCommitFreq) {
		this.extractCommitFreq = extractCommitFreq;
	}

	public BigDecimal getExtractMaxRecPerFile() {
		return this.extractMaxRecPerFile;
	}

	public void setExtractMaxRecPerFile(BigDecimal extractMaxRecPerFile) {
		this.extractMaxRecPerFile = extractMaxRecPerFile;
	}

	public String getExtractSql() {
		return this.extractSql;
	}

	public void setExtractSql(String extractSql) {
		this.extractSql = extractSql;
	}

	public String getLongDesc() {
		return this.longDesc;
	}

	public void setLongDesc(String longDesc) {
		this.longDesc = longDesc;
	}

	public String getOutputFileFormat() {
		return this.outputFileFormat;
	}

	public void setOutputFileFormat(String outputFileFormat) {
		this.outputFileFormat = outputFileFormat;
	}

	public String getOutputFilenamePostfix() {
		return this.outputFilenamePostfix;
	}

	public void setOutputFilenamePostfix(String outputFilenamePostfix) {
		this.outputFilenamePostfix = outputFilenamePostfix;
	}

	public String getOutputFilenamePrefix() {
		return this.outputFilenamePrefix;
	}

	public void setOutputFilenamePrefix(String outputFilenamePrefix) {
		this.outputFilenamePrefix = outputFilenamePrefix;
	}

	public String getParam1() {
		return this.param1;
	}

	public void setParam1(String param1) {
		this.param1 = param1;
	}

	public String getParam2() {
		return this.param2;
	}

	public void setParam2(String param2) {
		this.param2 = param2;
	}

	public String getParam3() {
		return this.param3;
	}

	public void setParam3(String param3) {
		this.param3 = param3;
	}

	public String getShortDesc() {
		return this.shortDesc;
	}

	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<JobStepsXref> getJobStepsXrefs() {
		return this.jobStepsXrefs;
	}

	public void setJobStepsXrefs(List<JobStepsXref> jobStepsXrefs) {
		this.jobStepsXrefs = jobStepsXrefs;
	}

	public JobStepsXref addJobStepsXref(JobStepsXref jobStepsXref) {
		getJobStepsXrefs().add(jobStepsXref);
		jobStepsXref.setStep(this);

		return jobStepsXref;
	}

	public JobStepsXref removeJobStepsXref(JobStepsXref jobStepsXref) {
		getJobStepsXrefs().remove(jobStepsXref);
		jobStepsXref.setStep(null);

		return jobStepsXref;
	}

}