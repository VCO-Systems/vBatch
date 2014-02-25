package model;

import java.io.Serializable;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.Date;


/**
 * The persistent class for the BATCH_LOG_DTL database table.
 * 
 */
@Entity
@Table(name="BATCH_LOG_DTL")
@NamedQuery(name="BatchLogDtl.findAll", query="SELECT b FROM BatchLogDtl b")
public class BatchLogDtl implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(generator="BatchLogDtlGen")
	@SequenceGenerator(name="BatchLogDtlGen", sequenceName="BATCH_LOG_DTL_ID_SEQ", allocationSize=1)
	@Column(unique=true, nullable=false)
	private long id;

	@Column(name="CLASS_PATH", length=150)
	private String classPath;

	@Temporal(TemporalType.DATE)
	@Column(name="END_DT")
	private Date endDt;

	@Column(name="ERROR_MSG", length=150)
	private String errorMsg;

	@Column(name="EXTRACT_COMMIT_FREQ")
	private BigDecimal extractCommitFreq;

	@Column(name="EXTRACT_MAX_RECS_PER_FILE")
	private BigDecimal extractMaxRecsPerFile;

	@Column(name="EXTRACT_SQL", length=4000)
	private String extractSql;

	@Column(name="JOB_STEPS_XREF_JOB_STEP_SEQ")
	private BigDecimal jobStepsXrefJobStepSeq;

	@Column(name="LONG_DESC", length=150)
	private String longDesc;

	@Column(name="MAX_OK2", length=150)
	private String maxOk2;

	@Column(name="MIN_OK1", length=150)
	private String minOk1;

	@Column(name="NUM_RECORDS")
	private BigDecimal numRecords;

	@Column(name="OUTPUT_FILE_FORMAT", length=15)
	private String outputFileFormat;

	@Column(name="OUTPUT_FILENAME_PREFIX", length=150)
	private String outputFilenamePrefix;

	@Column(name="OUTPUT_FILENAME_SUFFIX", length=150)
	private String outputFilenameSuffix;

	@Column(length=150)
	private String param1;

	@Column(length=150)
	private String param2;

	@Column(length=150)
	private String param3;

	@Temporal(TemporalType.DATE)
	@Column(name="START_DT")
	private Date startDt;

	@Column(length=50)
	private String status;

	@Column(name="STEP_TYPE", length=150)
	private String stepType;

	@Column(name="STEPS_ID")
	private BigDecimal stepsId;

	@Column(name="STEPS_SHORT_DESC", length=20)
	private String stepsShortDesc;

	//bi-directional many-to-one association to BatchLog
	@ManyToOne
	@JoinColumn(name="BATCH_LOG_ID", nullable=false)
	private BatchLog batchLog;

	public BatchLogDtl() {
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

	public Date getEndDt() {
		return this.endDt;
	}

	public void setEndDt(Date endDt) {
		this.endDt = endDt;
	}

	public String getErrorMsg() {
		return this.errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	public BigDecimal getExtractCommitFreq() {
		return this.extractCommitFreq;
	}

	public void setExtractCommitFreq(BigDecimal extractCommitFreq) {
		this.extractCommitFreq = extractCommitFreq;
	}

	public BigDecimal getExtractMaxRecsPerFile() {
		return this.extractMaxRecsPerFile;
	}

	public void setExtractMaxRecsPerFile(BigDecimal extractMaxRecsPerFile) {
		this.extractMaxRecsPerFile = extractMaxRecsPerFile;
	}

	public String getExtractSql() {
		return this.extractSql;
	}

	public void setExtractSql(String extractSql) {
		this.extractSql = extractSql;
	}

	public BigDecimal getJobStepsXrefJobStepSeq() {
		return this.jobStepsXrefJobStepSeq;
	}

	public void setJobStepsXrefJobStepSeq(BigDecimal jobStepsXrefJobStepSeq) {
		this.jobStepsXrefJobStepSeq = jobStepsXrefJobStepSeq;
	}

	public String getLongDesc() {
		return this.longDesc;
	}

	public void setLongDesc(String longDesc) {
		this.longDesc = longDesc;
	}

	public String getMaxOk2() {
		return this.maxOk2;
	}

	public void setMaxOk2(String maxOk2) {
		this.maxOk2 = maxOk2;
	}

	public String getMinOk1() {
		return this.minOk1;
	}

	public void setMinOk1(String minOk1) {
		this.minOk1 = minOk1;
	}

	public BigDecimal getNumRecords() {
		return this.numRecords;
	}

	public void setNumRecords(BigDecimal numRecords) {
		this.numRecords = numRecords;
	}

	public String getOutputFileFormat() {
		return this.outputFileFormat;
	}

	public void setOutputFileFormat(String outputFileFormat) {
		this.outputFileFormat = outputFileFormat;
	}

	public String getOutputFilenamePrefix() {
		return this.outputFilenamePrefix;
	}

	public void setOutputFilenamePrefix(String outputFilenamePrefix) {
		this.outputFilenamePrefix = outputFilenamePrefix;
	}

	public String getOutputFilenameSuffix() {
		return this.outputFilenameSuffix;
	}

	public void setOutputFilenameSuffix(String outputFilenameSuffix) {
		this.outputFilenameSuffix = outputFilenameSuffix;
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

	public Date getStartDt() {
		return this.startDt;
	}

	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStepType() {
		return this.stepType;
	}

	public void setStepType(String stepType) {
		this.stepType = stepType;
	}

	public BigDecimal getStepsId() {
		return this.stepsId;
	}

	public void setStepsId(BigDecimal stepsId) {
		this.stepsId = stepsId;
	}

	public String getStepsShortDesc() {
		return this.stepsShortDesc;
	}

	public void setStepsShortDesc(String stepsShortDesc) {
		this.stepsShortDesc = stepsShortDesc;
	}

	public BatchLog getBatchLog() {
		return this.batchLog;
	}

	public void setBatchLog(BatchLog batchLog) {
		this.batchLog = batchLog;
	}

}