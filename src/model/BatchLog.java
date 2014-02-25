package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the BATCH_LOG database table.
 * 
 */
@Entity
@Table(name="BATCH_LOG")
@NamedQuery(name="BatchLog.findAll", query="SELECT b FROM BatchLog b")
public class BatchLog implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(generator="BatchLogGen")
	@SequenceGenerator(name="BatchLogGen", sequenceName="BATCH_LOG_ID_SEQ", allocationSize=1)
	@Column(unique=true, nullable=false)
	private long id;

	@Column(name="BATCH_NUM", nullable=false)
	private BigDecimal batchNum;

	@Column(name="BATCH_SEQ_NBR", nullable=false)
	private BigDecimal batchSeqNbr;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="END_DT")
	private Date endDt;

	@Column(name="ERROR_MSG", length=4000)
	private String errorMsg;

	@Column(name="LONG_DESC", length=150)
	private String longDesc;

	@Column(name="ORDER_NUM")
	private BigDecimal orderNum;

	@Column(name="SHORT_DESC", length=20)
	private String shortDesc;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="START_DT")
	private Date startDt;

	@Column(name="VBATCH_LOG_STATUS", length=50)
	private String vbatchLogStatus;

	//bi-directional many-to-one association to JobDefinition
	@ManyToOne
	@JoinColumn(name="JOB_DEFINITION_ID", nullable=false)
	private JobDefinition jobDefinition;

	//bi-directional many-to-one association to BatchLogDtl
	@OneToMany(mappedBy="batchLog")
	private List<BatchLogDtl> batchLogDtls;

	//bi-directional many-to-one association to BatchLogFileOutput
	@OneToMany(mappedBy="batchLog")
	private List<BatchLogFileOutput> batchLogFileOutputs;

	//bi-directional many-to-one association to BatchLogOkDtl
	@OneToMany(mappedBy="batchLog")
	private List<BatchLogOkDtl> batchLogOkDtls;

	public BatchLog() {
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

	public BigDecimal getBatchSeqNbr() {
		return this.batchSeqNbr;
	}

	public void setBatchSeqNbr(BigDecimal batchSeqNbr) {
		this.batchSeqNbr = batchSeqNbr;
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

	public Date getStartDt() {
		return this.startDt;
	}

	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}

	public String getVbatchLogStatus() {
		return this.vbatchLogStatus;
	}

	public void setVbatchLogStatus(String vbatchLogStatus) {
		this.vbatchLogStatus = vbatchLogStatus;
	}

	public JobDefinition getJobDefinition() {
		return this.jobDefinition;
	}

	public void setJobDefinition(JobDefinition jobDefinition) {
		this.jobDefinition = jobDefinition;
	}

	public List<BatchLogDtl> getBatchLogDtls() {
		return this.batchLogDtls;
	}

	public void setBatchLogDtls(List<BatchLogDtl> batchLogDtls) {
		this.batchLogDtls = batchLogDtls;
	}

	public BatchLogDtl addBatchLogDtl(BatchLogDtl batchLogDtl) {
		getBatchLogDtls().add(batchLogDtl);
		batchLogDtl.setBatchLog(this);

		return batchLogDtl;
	}

	public BatchLogDtl removeBatchLogDtl(BatchLogDtl batchLogDtl) {
		getBatchLogDtls().remove(batchLogDtl);
		batchLogDtl.setBatchLog(null);

		return batchLogDtl;
	}

	public List<BatchLogFileOutput> getBatchLogFileOutputs() {
		return this.batchLogFileOutputs;
	}

	public void setBatchLogFileOutputs(List<BatchLogFileOutput> batchLogFileOutputs) {
		this.batchLogFileOutputs = batchLogFileOutputs;
	}

	public BatchLogFileOutput addBatchLogFileOutput(BatchLogFileOutput batchLogFileOutput) {
		getBatchLogFileOutputs().add(batchLogFileOutput);
		batchLogFileOutput.setBatchLog(this);

		return batchLogFileOutput;
	}

	public BatchLogFileOutput removeBatchLogFileOutput(BatchLogFileOutput batchLogFileOutput) {
		getBatchLogFileOutputs().remove(batchLogFileOutput);
		batchLogFileOutput.setBatchLog(null);

		return batchLogFileOutput;
	}

	public List<BatchLogOkDtl> getBatchLogOkDtls() {
		return this.batchLogOkDtls;
	}

	public void setBatchLogOkDtls(List<BatchLogOkDtl> batchLogOkDtls) {
		this.batchLogOkDtls = batchLogOkDtls;
	}

	public BatchLogOkDtl addBatchLogOkDtl(BatchLogOkDtl batchLogOkDtl) {
		getBatchLogOkDtls().add(batchLogOkDtl);
		batchLogOkDtl.setBatchLog(this);

		return batchLogOkDtl;
	}

	public BatchLogOkDtl removeBatchLogOkDtl(BatchLogOkDtl batchLogOkDtl) {
		getBatchLogOkDtls().remove(batchLogOkDtl);
		batchLogOkDtl.setBatchLog(null);

		return batchLogOkDtl;
	}

}