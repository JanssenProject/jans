import { styled } from '@mui/material/styles';
import TableCell, { tableCellClasses, TableCellProps } from '@mui/material/TableCell';

type StyledTableCellProps = TableCellProps & {
  headerColor?: 'primaryDark' | 'black';
};

const StyledTableCell = styled(TableCell, {
  shouldForwardProp: (prop) => prop !== 'headerColor',
})<StyledTableCellProps>(({ theme, headerColor = 'primaryDark' }) => ({
  [`&.${tableCellClasses.head}`]: {
    backgroundColor:
      headerColor === 'black'
        ? theme.palette.common.black
        : theme.palette.primary.dark,
    color: theme.palette.common.white,
    fontWeight: 700,
  },
  [`&.${tableCellClasses.body}`]: { fontSize: 14 },
}));

export default StyledTableCell;
