package model;

import java.io.Serializable;
import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;


/**
 * The persistent class for the VBATCH_LOG_FILE_OUTPUT database table.
 * 
 */
@Entity
@Table(name="VBATCH_LOG_FILE_OUTPUT")
@NamedQuery(name="VbatchLogFileOutput.findAll", query="SELECT v FROM VbatchLogFileOutput v")
public class VbatchLogFileOutput implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@Column(unique=true, nullable=false)
	private long id;

	@Temporal(TemporalType.DATE)
	@Column(name="CREATE_DT", nullable=false)
	private Date createDt;

	@Column(nullable=false, length=150)
	private String filename;

	@Column(name="LOG_DTL_SEQ_NBR")
	private BigDecimal logDtlSeqNbr;

	@Column(name="NUM_RECORDS", nullable=false)
	private BigDecimal numRecords;

	//bi-directional many-to-one association to VbatchLog
	@ManyToOne
	@JoinColumn(name="VBATCH_LOG_ID", nullable=false)
	private VbatchLog vbatchLog;

	public VbatchLogFileOutput() {
	}

	public long getId() {
		return this.id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Date getCreateDt() {
		return this.createDt;
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	public String getFilename() {
		return this.filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public BigDecimal getLogDtlSeqNbr() {
		return this.logDtlSeqNbr;
	}

	public void setLogDtlSeqNbr(BigDecimal logDtlSeqNbr) {
		this.logDtlSeqNbr = logDtlSeqNbr;
	}

	public BigDecimal getNumRecords() {
		return this.numRecords;
	}

	public void setNumRecords(BigDecimal numRecords) {
		this.numRecords = numRecords;
	}

	public VbatchLog getVbatchLog() {
		return this.vbatchLog;
	}

	public void setVbatchLog(VbatchLog vbatchLog) {
		this.vbatchLog = vbatchLog;
	}

}