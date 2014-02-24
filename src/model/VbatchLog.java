package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * The persistent class for the VBATCH_LOG database table.
 * 
 */
@Entity
@Table(name="VBATCH_LOG")
@NamedQuery(name="VbatchLog.findAll", query="SELECT v FROM VbatchLog v")
public class VbatchLog implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Column(name="BATCH_NUM", nullable=false)
	private BigDecimal batchNum;

	@Column(name="BATCH_SEQ_NBR", nullable=false)
	private BigDecimal batchSeqNbr;

	@Temporal(TemporalType.DATE)
	@Column(name="VBATCH_LOG_END_DT")
	private Date vbatchLogEndDt;

	@Temporal(TemporalType.DATE)
	@Column(name="VBATCH_LOG_START_DT")
	private Date vbatchLogStartDt;

	@Column(name="VBATCH_LOG_STATUS", nullable=false, length=50)
	private String vbatchLogStatus;

	//bi-directional many-to-one association to JobMaster
	@ManyToOne
	@JoinColumn(name="JOB_MASTER_ID", nullable=false)
	private JobMaster jobMaster;

	//bi-directional many-to-one association to VbatchLogDtl
	@OneToMany(mappedBy="vbatchLog")
	private List<VbatchLogDtl> vbatchLogDtls;

	//bi-directional many-to-one association to VbatchLogFileOutput
	@OneToMany(mappedBy="vbatchLog")
	private List<VbatchLogFileOutput> vbatchLogFileOutputs;

	//bi-directional many-to-one association to VbatchLogOkDtl
	@OneToMany(mappedBy="vbatchLog")
	private List<VbatchLogOkDtl> vbatchLogOkDtls;

	public VbatchLog() {
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

	public Date getVbatchLogEndDt() {
		return this.vbatchLogEndDt;
	}

	public void setVbatchLogEndDt(Date vbatchLogEndDt) {
		this.vbatchLogEndDt = vbatchLogEndDt;
	}

	public Date getVbatchLogStartDt() {
		return this.vbatchLogStartDt;
	}

	public void setVbatchLogStartDt(Date vbatchLogStartDt) {
		this.vbatchLogStartDt = vbatchLogStartDt;
	}

	public String getVbatchLogStatus() {
		return this.vbatchLogStatus;
	}

	public void setVbatchLogStatus(String vbatchLogStatus) {
		this.vbatchLogStatus = vbatchLogStatus;
	}

	public JobMaster getJobMaster() {
		return this.jobMaster;
	}

	public void setJobMaster(JobMaster jobMaster) {
		this.jobMaster = jobMaster;
	}

	public List<VbatchLogDtl> getVbatchLogDtls() {
		return this.vbatchLogDtls;
	}

	public void setVbatchLogDtls(List<VbatchLogDtl> vbatchLogDtls) {
		this.vbatchLogDtls = vbatchLogDtls;
	}

	public VbatchLogDtl addVbatchLogDtl(VbatchLogDtl vbatchLogDtl) {
		getVbatchLogDtls().add(vbatchLogDtl);
		vbatchLogDtl.setVbatchLog(this);

		return vbatchLogDtl;
	}

	public VbatchLogDtl removeVbatchLogDtl(VbatchLogDtl vbatchLogDtl) {
		getVbatchLogDtls().remove(vbatchLogDtl);
		vbatchLogDtl.setVbatchLog(null);

		return vbatchLogDtl;
	}

	public List<VbatchLogFileOutput> getVbatchLogFileOutputs() {
		return this.vbatchLogFileOutputs;
	}

	public void setVbatchLogFileOutputs(List<VbatchLogFileOutput> vbatchLogFileOutputs) {
		this.vbatchLogFileOutputs = vbatchLogFileOutputs;
	}

	public VbatchLogFileOutput addVbatchLogFileOutput(VbatchLogFileOutput vbatchLogFileOutput) {
		getVbatchLogFileOutputs().add(vbatchLogFileOutput);
		vbatchLogFileOutput.setVbatchLog(this);

		return vbatchLogFileOutput;
	}

	public VbatchLogFileOutput removeVbatchLogFileOutput(VbatchLogFileOutput vbatchLogFileOutput) {
		getVbatchLogFileOutputs().remove(vbatchLogFileOutput);
		vbatchLogFileOutput.setVbatchLog(null);

		return vbatchLogFileOutput;
	}

	public List<VbatchLogOkDtl> getVbatchLogOkDtls() {
		return this.vbatchLogOkDtls;
	}

	public void setVbatchLogOkDtls(List<VbatchLogOkDtl> vbatchLogOkDtls) {
		this.vbatchLogOkDtls = vbatchLogOkDtls;
	}

	public VbatchLogOkDtl addVbatchLogOkDtl(VbatchLogOkDtl vbatchLogOkDtl) {
		getVbatchLogOkDtls().add(vbatchLogOkDtl);
		vbatchLogOkDtl.setVbatchLog(this);

		return vbatchLogOkDtl;
	}

	public VbatchLogOkDtl removeVbatchLogOkDtl(VbatchLogOkDtl vbatchLogOkDtl) {
		getVbatchLogOkDtls().remove(vbatchLogOkDtl);
		vbatchLogOkDtl.setVbatchLog(null);

		return vbatchLogOkDtl;
	}

}