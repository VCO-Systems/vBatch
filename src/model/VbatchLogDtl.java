package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;


/**
 * The persistent class for the VBATCH_LOG_DTL database table.
 * 
 */
@Entity
@Table(name="VBATCH_LOG_DTL")
@NamedQuery(name="VbatchLogDtl.findAll", query="SELECT v FROM VbatchLogDtl v")
public class VbatchLogDtl implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(name="BATCH_NUM", nullable=false)
	private BigDecimal batchNum;

	@Column(nullable=false, length=150)
	private String description;

	@Column(name="ERROR_MSG", nullable=false, length=150)
	private String errorMsg;

	@Column(name="EXTRACT_COMMIT_FREQ", nullable=false)
	private BigDecimal extractCommitFreq;

	@Column(name="EXTRACT_MAX_RECS_PER_FILE", nullable=false)
	private BigDecimal extractMaxRecsPerFile;

	@Column(name="EXTRACT_SQL", nullable=false, length=4000)
	private String extractSql;

	@Column(name="JAVA_BEAN_PATH", nullable=false, length=150)
	private String javaBeanPath;

	@Column(name="JOB_SEQ")
	private BigDecimal jobSeq;

	@Column(name="JOB_STEP_ID", nullable=false)
	private BigDecimal jobStepId;

	@Temporal(TemporalType.DATE)
	@Column(name="LOG_DTL_END_DT")
	private Date logDtlEndDt;

	@Temporal(TemporalType.DATE)
	@Column(name="LOG_DTL_START_DT")
	private Date logDtlStartDt;

	@Column(name="MAX_OK2", nullable=false, length=150)
	private String maxOk2;

	@Column(name="MIN_OK1", nullable=false, length=150)
	private String minOk1;

	@Column(name="NUM_RECORDS", nullable=false)
	private BigDecimal numRecords;

	@Column(name="OUTPUT_FILE_FORMAT", nullable=false, length=15)
	private String outputFileFormat;

	@Column(name="OUTPUT_FILENAME_PREFIX", nullable=false, length=150)
	private String outputFilenamePrefix;

	@Column(name="OUTPUT_FILENAME_SUFFIX", nullable=false, length=150)
	private String outputFilenameSuffix;

	@Column(nullable=false, length=150)
	private String param1;

	@Column(nullable=false, length=150)
	private String param2;

	@Column(nullable=false, length=150)
	private String param3;

	@Column(nullable=false, length=50)
	private String status;

	@Column(name="STEP_NAME", nullable=false)
	private BigDecimal stepName;

	@Column(name="STEP_TYPE", nullable=false, length=150)
	private String stepType;

	//bi-directional many-to-one association to VbatchLog
	@ManyToOne
	@JoinColumn(name="VBATCH_LOG_ID", nullable=false)
	private VbatchLog vbatchLog;

	public VbatchLogDtl() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public BigDecimal getBatchNum() {
		return this.batchNum;
	}

	public void setBatchNum(BigDecimal batchNum) {
		this.batchNum = batchNum;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public String getJavaBeanPath() {
		return this.javaBeanPath;
	}

	public void setJavaBeanPath(String javaBeanPath) {
		this.javaBeanPath = javaBeanPath;
	}

	public BigDecimal getJobSeq() {
		return this.jobSeq;
	}

	public void setJobSeq(BigDecimal jobSeq) {
		this.jobSeq = jobSeq;
	}

	public BigDecimal getJobStepId() {
		return this.jobStepId;
	}

	public void setJobStepId(BigDecimal jobStepId) {
		this.jobStepId = jobStepId;
	}

	public Date getLogDtlEndDt() {
		return this.logDtlEndDt;
	}

	public void setLogDtlEndDt(Date logDtlEndDt) {
		this.logDtlEndDt = logDtlEndDt;
	}

	public Date getLogDtlStartDt() {
		return this.logDtlStartDt;
	}

	public void setLogDtlStartDt(Date logDtlStartDt) {
		this.logDtlStartDt = logDtlStartDt;
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

	public String getStatus() {
		return this.status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public BigDecimal getStepName() {
		return this.stepName;
	}

	public void setStepName(BigDecimal stepName) {
		this.stepName = stepName;
	}

	public String getStepType() {
		return this.stepType;
	}

	public void setStepType(String stepType) {
		this.stepType = stepType;
	}

	public VbatchLog getVbatchLog() {
		return this.vbatchLog;
	}

	public void setVbatchLog(VbatchLog vbatchLog) {
		this.vbatchLog = vbatchLog;
	}

}